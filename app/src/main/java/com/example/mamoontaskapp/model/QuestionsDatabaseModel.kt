package com.example.mamoontaskapp.model

import com.example.mamoontaskapp.repository.database.Answer


data class QuestionsDatabaseModel(
    val formName: String,
    val questionGroups: List<QuestionsDataBaseGroup>,
    val images: List<ImagesDataBase>
)
data class ImagesDataBase(
    val id: Int,
    val uri: String,
    val questionId: Long
)
data class QuestionsDataBaseGroup(
    val id: Int,
    val name: String,
    val questions: List<QuestionDataBase>
)

data class QuestionDataBase(
    val id: Int,
    val text: String,
    var answer: Answer,
    val isDone: Boolean,
    var description: String?,
    var isExpanded: Boolean
)