package com.example.mamoontaskapp.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mamoontaskapp.adapters.DescriptionChangeListener
import com.example.mamoontaskapp.adapters.OnAnswerChange
import com.example.mamoontaskapp.adapters.OnExpandChange
import com.example.mamoontaskapp.model.QuestionDataBase
import com.example.mamoontaskapp.model.QuestionsDatabaseModel
import com.example.mamoontaskapp.repository.QuestionsRepository
import com.example.mamoontaskapp.repository.database.Answer
import com.example.mamoontaskapp.repository.database.ImageEntity
import com.example.mamoontaskapp.repository.database.QuestionDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuestionsViewModel(application: Application) : AndroidViewModel(application),
    DescriptionChangeListener, OnAnswerChange, OnExpandChange {
    private val questionsRepository: QuestionsRepository = QuestionsRepository(
        application, QuestionDatabase.getDatabase(application).questionDao()
    )

    private val _imageUpdates = MutableSharedFlow<QuestionWithImage>() // Emits when a new image is added
    val imageUpdates: SharedFlow<QuestionWithImage> = _imageUpdates

    suspend fun addPhoto(questionWithImages: List<QuestionWithImage>) {
        questionWithImages.forEach { questionWithImage ->
            questionsRepository.insertImage(questionWithImage.toImageEntity())
        }
    }

    fun loadQuestionsFromJson() {
        viewModelScope.launch(Dispatchers.IO) {
            val questionsModel = questionsRepository.getQuestionsFromJson(getApplication())
            questionsModel?.let { questionsRepository.saveQuestions(it) }
        }
    }

    suspend fun loadQuestionsFromDatabase(): QuestionsDatabaseModel {
        Log.d(TAG, "Loading questions from database")
        return withContext(Dispatchers.IO) {
            questionsRepository.loadQuestionsFromDataBase()
        }
    }

    suspend fun loadImagesFromDatabase(): List<ImageEntity> {
        Log.d(TAG, "Loading images from database")
        return withContext(Dispatchers.IO) {
            questionsRepository.getAllImages()
        }
    }

     fun loadImagesFromDatabaseFlow(): Flow<List<QuestionWithImage>> =
        questionsRepository.getAllImagesFlow()
            .map { imageEntities ->
                imageEntities.map { imageEntity ->
                    QuestionWithImage(
                        questionId = imageEntity.questionId.toInt(),
                        imageUri = Uri.parse(imageEntity.uri)
                    )
                }
            }
            .flowOn(Dispatchers.IO) // Dispatchers.IO is used for the collection of the flow

    override fun onDescriptionChange(question: QuestionDataBase, newDescription: String) {
        viewModelScope.launch {
            // Read the existing question from the database
            val existingQuestion = questionsRepository.getQuestion(question.id)

            // Only update the description, keep other fields as is
            if (existingQuestion != null) {
                questionsRepository.updateQuestionDescription(
                    existingQuestion.id,
                    newDescription
                )

                // Load updated question groups from the database
            }
        }
    }

    override fun onExpandChange(question: QuestionDataBase, isExpanded: Boolean) {
        viewModelScope.launch {
            // Read the existing question from the database
            val existingQuestion = questionsRepository.getQuestion(question.id)

            // Only update the expand status, keep other fields as is
            if (existingQuestion != null) {
                questionsRepository.updateQuestionIsExpanded(
                    existingQuestion.id,
                    isExpanded
                )

                // Load updated question groups from the database
            }
        }
    }

    override fun onAnswerChange(question: QuestionDataBase, answer: Answer) {
        viewModelScope.launch {
            // Update the question in the database
            val existingQuestion = questionsRepository.getQuestion(question.id)
            if (existingQuestion != null) {
                questionsRepository.updateQuestion(existingQuestion.id, answer)
            }
        }
    }

    companion object {
        private const val TAG = "QuestionsViewModel"
    }
}

data class QuestionWithImage(
    val questionId: Int,
    val imageUri: Uri
)

fun QuestionWithImage.toImageEntity(): ImageEntity {
    return ImageEntity(
        id = 0, // Set the ID as per your requirement
        uri = imageUri.toString(),
        questionId = questionId.toLong()
    )
}