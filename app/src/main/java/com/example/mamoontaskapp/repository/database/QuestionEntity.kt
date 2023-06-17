package com.example.mamoontaskapp.repository.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
enum class Answer {
    YES,
    NO,
    UNANSWERED
}
@Entity(
    tableName = "image",
    foreignKeys = [ForeignKey(
        entity = QuestionEntity::class,
        parentColumns = ["id"],
        childColumns = ["questionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uri: String,
    val questionId: Long
)

@Entity(tableName = "form")
data class FormEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val formName: String
)

@Entity(
    tableName = "question_group",
    foreignKeys = [ForeignKey(
        entity = FormEntity::class,
        parentColumns = ["id"],
        childColumns = ["formId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class QuestionGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val formId: Long
)

@Entity(
    tableName = "question",
    foreignKeys = [ForeignKey(
        entity = QuestionGroupEntity::class,
        parentColumns = ["id"],
        childColumns = ["groupId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    var description: String?,
    val answer: Answer,
    val isDone: Boolean,
    val groupId: Long,
    var isExpanded: Boolean,

)