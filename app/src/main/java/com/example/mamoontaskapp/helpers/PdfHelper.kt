import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.example.mamoontaskapp.helpers.NotificationHelper
import com.example.mamoontaskapp.model.QuestionsDatabaseModel
import com.example.mamoontaskapp.repository.database.ImageEntity
import com.example.mamoontaskapp.viewmodel.QuestionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfHelper(private val questionsViewModel: QuestionsViewModel, private val context: Context, private val lifecycleScope: LifecycleCoroutineScope)
{
    suspend fun createPDFFile(
        context: Context,
        questionsModel: QuestionsDatabaseModel,
        images: List<ImageEntity>
    ): File {
        val pdfFileName = "generated_pdf.pdf"
        val pdfFilePath = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            pdfFileName
        )
        val pdfDocument = PdfDocument()

        try {
            val paint = Paint()
            paint.textSize = 12f
            paint.color = Color.BLACK

            var pageNumber = 1
            var page: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var y = 50f

            val dateText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            questionsModel.questionGroups.forEach { questionGroup ->
                // Draw the question group name and line separator
                canvas?.drawText(questionGroup.name, 20f, y, paint)
                canvas?.drawLine(20f, y + 10f, 280f, y + 10f, paint)
                y += 30f

                questionGroup.questions.forEach { question ->
                    val description = question.description
                    val photoNumber = images.count { it.questionId == question.id.toLong() }

                    // Check if a new page needs to be started
                    if (page == null || y + 100f > page!!.info.pageHeight) {
                        if (page != null) {
                            // Finish the current page and increment the page number
                            pdfDocument.finishPage(page)
                            pageNumber++
                        }

                        // Start a new page and set the canvas
                        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page!!.canvas

                        y = 50f // reset y position for new page

                        // Draw the date at the top of the page
                        val pageWidth = page?.info?.pageWidth ?: 0
                        val textWidth = paint.measureText(dateText)
                        val x = pageWidth - textWidth - 20f
                        canvas?.drawText(dateText, x, 20f, paint)
                    }

                    // Combine the question text and answer (if answered)
                    val questionWithAnswer = if (question.answer.toString() != "UNANSWERED") {
                        "${question.text}   ${question.answer}"
                    } else {
                        question.text
                    }

                    val textWidth = 260f // Adjust this value based on your page width
                    val textPaint = TextPaint(paint)

                    // Create a StaticLayout instance with the question and answer text
                    val staticLayout = StaticLayout.Builder
                        .obtain(questionWithAnswer, 0, questionWithAnswer.length, textPaint, textWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()

                    // Draw the StaticLayout on the canvas
                    canvas?.save()
                    canvas?.translate(20f, y)
                    staticLayout.draw(canvas)
                    canvas?.restore()

                    // Update the y position based on the height of the StaticLayout
                    y += staticLayout.height

                    // Draw the question description (if exists)
                    if (description != null) {
                        canvas?.drawText(description, 20f, y + 20f, paint)
                        y += 20f
                    }

                    // Draw the label for the photos related to the question
                    val photoLabel = "Photo"
                    val photoText = "$photoLabel ($photoNumber)"
                    canvas?.drawText(photoText, 20f, y + 20f, paint)
                    y += 50f

                    // Iterate over images related to the current question
                    images.filter { it.questionId == question.id.toLong() }.forEachIndexed { index, image ->
                        if (y + 100f > page!!.info.pageHeight) {
                            // Finish the current page and increment the page number
                            pdfDocument.finishPage(page)
                            pageNumber++

                            // Start a new page and set the canvas
                            val pageInfo = PdfDocument.PageInfo.Builder(300, 600, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page!!.canvas

                            y = 50f // reset y position for new page

                            // Draw the date at the top of the page
                            val pageWidth = page?.info?.pageWidth ?: 0
                            val textWidth = paint.measureText(dateText)
                            val x = pageWidth - textWidth - 20f
                            canvas?.drawText(dateText, x, 20f, paint)
                        }

                        // Load the photo bitmap
                        val photoBitmap = loadPhotoBitmap(context, image.uri.toUri(), 200)
                        Log.d(TAG, "Here's the URI $image")

                        if (photoBitmap != null) {
                            // Resize the photo bitmap if needed
                            val maxWidth = 200
                            val scaledWidth: Int
                            val scaledHeight: Int
                            if (photoBitmap.width > maxWidth) {
                                val scaleFactor = maxWidth.toFloat() / photoBitmap.width.toFloat()
                                scaledWidth = maxWidth
                                scaledHeight = (photoBitmap.height * scaleFactor).toInt()
                            } else {
                                scaledWidth = photoBitmap.width
                                scaledHeight = photoBitmap.height
                            }

                            val scaledBitmap = Bitmap.createScaledBitmap(photoBitmap, scaledWidth, scaledHeight, true)

                            // Draw the photo bitmap on the canvas
                            val photoX = 20f
                            val photoY = y
                            canvas?.drawBitmap(scaledBitmap, photoX, photoY, paint)

                            // Update the y position based on the height of the photo bitmap
                            y += scaledBitmap.height.toFloat() + 10f
                        } else {
                            Log.e(TAG, "Failed to load photo bitmap for image at index $index")
                        }
                    }

                    y += 20f // Increase y position for the next question
                }
            }

            // Finish the last page if exists
            if (page != null) {
                pdfDocument.finishPage(page)
            }

            // Write the PDF contents to a temporary file
            val tempFile = File(context.cacheDir, "temp_pdf.pdf")
            withContext(Dispatchers.IO) {
                pdfDocument.writeTo(FileOutputStream(tempFile))
            }

            // Copy the contents of the temporary file to the final PDF file
            val uri = FileProvider.getUriForFile(
                context,
                "com.example.mamoontaskapp.fileprovider",
                pdfFilePath
            )
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().copyTo(outputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        return pdfFilePath
    }
    private suspend fun loadPhotoBitmap(context: Context, imageUri: Uri, maxWidth: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val futureTarget: FutureTarget<Bitmap> = Glide.with(context)
                    .asBitmap()
                    .load(imageUri)
                    .submit(maxWidth, maxWidth)

                val bitmap = futureTarget.get()

                Glide.with(context).clear(futureTarget) // Clears resources associated with the FutureTarget.

                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bitmap from uri: $imageUri", e)
                null
            }
        }
    }

    fun generatePDFAndOpen() {
        lifecycleScope.launch {
            try {
                // Load questions from the database
                val questionsModel = questionsViewModel.loadQuestionsFromDatabase()
                Log.d(TAG, "Questions loaded: ${questionsModel.questionGroups.size}")

                // Load images from the database
                val images = questionsViewModel.loadImagesFromDatabase()
                Log.d(TAG, "Images loaded: ${images.size}")

                if (questionsModel.questionGroups.isNotEmpty()) {
                    // Generate a PDF file
                    val pdfFile: File = createPDFFile(context, questionsModel, images)

                    // Create an intent to open the generated PDF
                    val openIntent = Intent(Intent.ACTION_VIEW)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "com.example.mamoontaskapp.fileprovider",
                        pdfFile
                    )
                    openIntent.setDataAndType(uri, "application/pdf")
                    openIntent.flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP

                    // Create a pending intent that will be fired when the user taps the notification
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        openIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    // Initialize NotificationHelper and create the notification
                    val notificationHelper = NotificationHelper(context)
                    val title = "PDF Download"
                    val message = "Download complete. Tap to open."
                    notificationHelper.createNotification(title, message, pendingIntent)
                } else {
                    Log.e(TAG, "No questions or images found.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}