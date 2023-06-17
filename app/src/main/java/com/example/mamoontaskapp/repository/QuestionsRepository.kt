package com.example.mamoontaskapp.repository

import android.content.Context
import android.util.Log
import com.example.mamoontaskapp.model.ImagesDataBase
import com.example.mamoontaskapp.model.QuestionDataBase
import com.example.mamoontaskapp.model.QuestionsDataBaseGroup
import com.example.mamoontaskapp.model.QuestionsDatabaseModel
import com.example.mamoontaskapp.model.QuestionsModel
import com.example.mamoontaskapp.repository.database.Answer
import com.example.mamoontaskapp.repository.database.FormEntity
import com.example.mamoontaskapp.repository.database.ImageEntity
import com.example.mamoontaskapp.repository.database.QuestionDao
import com.example.mamoontaskapp.repository.database.QuestionEntity
import com.example.mamoontaskapp.repository.database.QuestionGroupEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException

class QuestionsRepository(private val context: Context, private val questionDao: QuestionDao) {

    /**
     * Load the questions from a JSON file.
     */
    fun getQuestionsFromJson(application: Any): QuestionsModel? {
        val fileName = "questions.json"
        val jsonString = loadJsonFromAsset(fileName)
        return jsonString?.let {
            val gson = Gson()
            val questionsModel = gson.fromJson(it, QuestionsModel::class.java)
            questionsModel
        }
    }

    /**
     * Get a question by its ID.
     */
    suspend fun getQuestion(questionId: Int): QuestionEntity? {
        Log.d(TAG, "Getting question with id: $questionId")
        return questionDao.getQuestionById(questionId)
    }

    /**
     * Get a flow of all image entities.
     */
    fun getAllImagesFlow(): Flow<List<ImageEntity>> {
        return questionDao.getAllImagesFlow()
    }

    /**
     * Get a list of all image entities.
     */
    fun getAllImages(): List<ImageEntity> {
        return questionDao.getAllImages()
    }

    /**
     * Save the questions to the database.
     */
    suspend fun saveQuestions(questionsModel: QuestionsModel) {
        Log.d(TAG, "Saving questions to the database")
        withContext(Dispatchers.IO) {
            val form = FormEntity(
                formName = questionsModel.formName
            )

            val formId = questionDao.insertForm(form)
            Log.d(TAG, "Form inserted with ID: $formId")

            val questionGroups = questionsModel.questionGroups.map { group ->
                QuestionGroupEntity(
                    formId = formId,
                    name = group.name
                )
            }

            val groupIds = questionDao.insertQuestionGroups(questionGroups)
            Log.d(TAG, "Question groups inserted with IDs: $groupIds")

            val questions = questionsModel.questionGroups.flatMapIndexed { index, group ->
                group.questions.map { question ->
                    QuestionEntity(
                        groupId = groupIds[index],
                        text = question.text,
                        description = "",
                        isDone = false,
                        isExpanded = false,
                        answer = Answer.UNANSWERED
                    )
                }
            }

            questionDao.insertQuestions(questions)
            Log.d(TAG, "Questions inserted: $questions")
        }
    }

    /**
     * Load the questions from the database.
     */
    suspend fun loadQuestionsFromDataBase(): QuestionsDatabaseModel {
        Log.d(TAG, "Loading questions from the database")
        return withContext(Dispatchers.IO) {
            val forms = questionDao.getAllForms()
            val images = questionDao.getAllImages()
            val questionsDatabaseModels = forms.map { form ->
                val questionGroups = questionDao.getQuestionGroupsForForm(form.id)
                QuestionsDatabaseModel(
                    formName = form.formName,
                    questionGroups = questionGroups.map { group ->
                        val questions = questionDao.getQuestionsForGroup(group.id)
                        QuestionsDataBaseGroup(
                            id = group.id,
                            name = group.name,
                            questions = questions.map { question ->
                                QuestionDataBase(
                                    id = question.id,
                                    text = question.text,
                                    answer = question.answer,
                                    isDone = question.isDone,
                                    description = question.description,
                                    isExpanded = question.isExpanded
                                )
                            }
                        )
                    },
                    images = images.map { image ->
                        ImagesDataBase(
                            id = image.id,
                            uri = image.uri,
                            questionId = image.questionId
                        )
                    }
                )
            }
            Log.d(TAG, "Questions loaded from the database: $questionsDatabaseModels")

            questionsDatabaseModels.firstOrNull() ?: QuestionsDatabaseModel(
                formName = "Default Form Name",
                questionGroups = emptyList(),
                images = emptyList()
            )
        }
    }

    /**
     * Load JSON data from an asset file.
     */
    private fun loadJsonFromAsset(fileName: String): String? {
        Log.d(TAG, "Loading JSON from asset: $fileName")
        var json: String? = null
        try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return json
    }

    /**
     * Update the description of a question.
     */
    suspend fun updateQuestionDescription(questionId: Int, description: String) {
        withContext(Dispatchers.IO) {
            questionDao.updateQuestionInTransaction {
                val existingQuestion = questionDao.getQuestionById(questionId)
                if (existingQuestion != null) {
                    val updatedQuestion = existingQuestion.copy(description = description)
                    questionDao.updateQuestion(updatedQuestion)
                    Log.d(
                        TAG,
                        "Question description updated for question id ${updatedQuestion.id} and the description is ${updatedQuestion.description}"
                    )
                }
            }
        }
    }

    /**
     * Update the isExpanded flag of a question.
     */
    suspend fun updateQuestionIsExpanded(questionId: Int, isExpanded: Boolean) {
        withContext(Dispatchers.IO) {
            questionDao.updateQuestionInTransaction {
                val existingQuestion = questionDao.getQuestionById(questionId)
                if (existingQuestion != null) {
                    val updatedQuestion = existingQuestion.copy(isExpanded = isExpanded)
                    questionDao.updateQuestion(updatedQuestion)
                    Log.d(
                        TAG,
                        "Question isExpanded updated for question id ${updatedQuestion.id} and the expand status is ${updatedQuestion.isExpanded}"
                    )
                }
            }
        }
    }

    /**
     * Update the answer of a question.
     */
    suspend fun updateQuestion(questionId: Int, answer: Answer) {
        withContext(Dispatchers.IO) {
            questionDao.updateQuestionInTransaction {
                val existingQuestion = questionDao.getQuestionById(questionId)
                if (existingQuestion != null) {
                    val updatedQuestion = existingQuestion.copy(answer = answer)
                    questionDao.updateQuestion(updatedQuestion)
                    Log.d(
                        TAG,
                        "Question Answer updated for question id ${updatedQuestion.id} and the answer status is ${updatedQuestion.answer}"
                    )
                }
            }
        }
    }

    /**
     * Insert an image entity.
     */
    suspend fun insertImage(insertImage: ImageEntity) {
        Log.d(TAG, "Inserting Image for id: ${insertImage.questionId}")
        withContext(Dispatchers.IO) {
            questionDao.insertImage(insertImage)
        }
    }

    companion object {
        private const val TAG = "QuestionsRepository"
    }
}