package com.example.mamoontaskapp.model

data class QuestionsModel(
    val formName: String,
    val questionGroups: List<QuestionsGroup>
)

data class QuestionsGroup(
    val id: Long,
    val name: String,
    val questions: List<Question>
)

data class Question(
    val id: Long,
    val text: String,
)
