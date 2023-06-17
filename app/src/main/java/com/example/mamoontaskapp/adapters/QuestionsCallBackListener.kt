package com.example.mamoontaskapp.adapters

import com.example.mamoontaskapp.model.QuestionDataBase
import com.example.mamoontaskapp.repository.database.Answer

interface DescriptionChangeListener {
    fun onDescriptionChange(question: QuestionDataBase, newDescription: String)
}

interface OnAnswerChange {
    fun onAnswerChange(question: QuestionDataBase, answer: Answer)
}

interface OnExpandChange {
    fun onExpandChange(question: QuestionDataBase, isExpanded: Boolean)
}
