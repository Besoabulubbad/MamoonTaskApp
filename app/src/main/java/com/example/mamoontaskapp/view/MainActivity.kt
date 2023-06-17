package com.example.mamoontaskapp.view

import PdfHelper
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mamoontaskapp.R
import com.example.mamoontaskapp.adapters.QuestionAdapter
import com.example.mamoontaskapp.adapters.QuestionGroupAdapter
import com.example.mamoontaskapp.model.QuestionDataBase
import com.example.mamoontaskapp.viewmodel.QuestionWithImage
import com.example.mamoontaskapp.viewmodel.QuestionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), QuestionAdapter.GalleryButtonListener {

    private val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 100
    private var isPermissionGranted = false
    private lateinit var questionsViewModel: QuestionsViewModel
    private lateinit var questionGroupAdapter: QuestionGroupAdapter
    private lateinit var getContent: ActivityResultLauncher<Array<String>>
    private var selectedQuestionId: Int? = null
    private var questionValue: QuestionDataBase? = null
    private lateinit var questionGroupRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if READ_EXTERNAL_STORAGE permission is granted
        checkStoragePermission()

        // Initialize ViewModel
        questionsViewModel = ViewModelProvider(this).get(QuestionsViewModel::class.java)
        questionGroupRecyclerView = findViewById(R.id.question_group_recycler_view)

        // Set up the generate button click listener
        val generateButton: TextView = findViewById(R.id.generate_text)
        generateButton.setOnClickListener {
            PdfHelper(questionsViewModel, this@MainActivity, lifecycleScope).generatePDFAndOpen()
        }

        // Register the OpenFileReceiver
        registerOpenFileReceiver()

        // Set up the question group recycler view
        setupQuestionGroupRecyclerView()

        // Load questions from json file and save to database when the app first starts
        questionsViewModel.loadQuestionsFromJson()

        // Register the ActivityResultContract
        registerContentActivityResultContract()

        // Load questions from database and populate the UI
        loadQuestionsFromDatabase()

        // Observe image updates for refreshing the adapter
        observeImageUpdates()
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is granted, set the flag
            isPermissionGranted = true
        } else {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setupQuestionGroupRecyclerView() {
        // Set up the question group recycler view
        questionGroupRecyclerView.layoutManager = LinearLayoutManager(this)

        // Set up the question group adapter
        questionGroupAdapter = QuestionGroupAdapter(questionsViewModel, this)
        questionGroupRecyclerView.adapter = questionGroupAdapter
    }

    private fun loadQuestionsFromDatabase() {
        lifecycleScope.launch(Dispatchers.Main) {
            val questionsModel = questionsViewModel.loadQuestionsFromDatabase()
            val formTitleTextView = findViewById<TextView>(R.id.form_title)
            formTitleTextView.text = questionsModel.formName

            questionGroupAdapter.setQuestionGroupsAndImages(
                questionsModel.questionGroups,
                questionsViewModel.loadImagesFromDatabaseFlow()
            )
        }
    }

    private fun observeImageUpdates() {
        lifecycleScope.launch {
            questionsViewModel.imageUpdates.collect { questionWithImage ->
                // Refresh the adapter for the question that has the new image
                val adapter = getAdapterForQuestion(questionWithImage.questionId)
                adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun getAdapterForQuestion(questionId: Int): QuestionAdapter? {
        return questionGroupAdapter.getQuestionAdapter(questionGroupRecyclerView, questionId)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, set the flag
                    isPermissionGranted = true

                    // Reload the questions or perform any other necessary tasks that require the permission
                    // Call the code to load questions here
                } else {
                    // Permission denied, show a message to the user indicating that the app can't perform without the permission
                    showToast("Permission denied. The app requires the READ_EXTERNAL_STORAGE permission to function properly.")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onGalleryButtonClick(question: QuestionDataBase, adapter: QuestionAdapter) {
        // Store the questionId to use it when an image is selected from the gallery
        selectedQuestionId = question.id
        questionValue = question

        // Launch the gallery picker
        getContent.launch(arrayOf("image/*"))
    }

    private fun registerOpenFileReceiver() {
        // Add a BroadcastReceiver to handle the open action
        val openFileReceiver = OpenFileReceiver()
        val intentFilter = IntentFilter(Intent.ACTION_VIEW)
        intentFilter.addDataScheme("content")
        intentFilter.addDataAuthority("com.example.mamoontaskapp.fileprovider", null)
        registerReceiver(openFileReceiver, intentFilter)
    }

    private fun registerContentActivityResultContract() {
        getContent = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                selectedQuestionId?.let { questionId ->
                    val questionWithImages = uris.map { uri -> QuestionWithImage(questionId, uri) }
                    lifecycleScope.launch {
                        try {
                            // Take persistable URI permission for each selected image
                            questionWithImages.forEach { questionWithImage ->
                                contentResolver.takePersistableUriPermission(
                                    questionWithImage.imageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                            }
                            questionsViewModel.addPhoto(questionWithImages)
                        } catch (e: SecurityException) {
                            contentResolver.persistedUriPermissions.forEach {
                                contentResolver.releasePersistableUriPermission(
                                    it.uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                            }
                            Log.e(TAG, "Permission denied when trying to add photos: $e")
                            // Handle the permission denial case (e.g., show an error message)
                        }
                    }
                } ?: run {
                    Log.e(TAG, "No selected questionId when trying to add photos")
                }
            } else {
                Log.e(TAG, "No URIs selected when trying to add photos")
            }
        }
    }

    // Add a BroadcastReceiver to handle the open action
    class OpenFileReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val openIntent = Intent(Intent.ACTION_VIEW)
            openIntent.data = intent.data
            openIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(openIntent)
            } catch (e: ActivityNotFoundException) {
                // Handle the case where no PDF viewer application is available
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the coroutine scope to avoid memory leaks
        lifecycleScope.cancel()
    }
}