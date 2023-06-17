package com.example.mamoontaskapp.repository.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.mamoontaskapp.viewmodel.QuestionWithImage
import kotlinx.coroutines.flow.Flow


@Dao
interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertForm(form: FormEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestionGroups(groups: List<QuestionGroupEntity>): List<Long>
    @Query("SELECT * FROM form")
    fun getAllFormsFlow(): Flow<List<FormEntity>>

    @Query("SELECT * FROM image")
    fun getAllImagesFlow(): Flow<List<ImageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateQuestion(question: QuestionEntity)

    @Transaction
    suspend fun updateQuestionInTransaction(transaction: suspend () -> Unit) {
        transaction()
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImage(question: ImageEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImages(images: List<ImageEntity>)
    @Query("SELECT * FROM form")
    fun getAllForms(): List<FormEntity>

    @Query("SELECT * FROM question_group WHERE formId = :formId")
    fun getQuestionGroupsForForm(formId: Int): List<QuestionGroupEntity>
    @Query("SELECT * FROM question_group WHERE formId = :formId")
    fun getQuestionGroupsForFormFlow(formId: Int): Flow<List<QuestionGroupEntity>>
    @Query("SELECT * FROM question WHERE groupId = :groupId")
    fun getQuestionsForGroupFlow(groupId: Int): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM question WHERE groupId = :groupId")
    fun getQuestionsForGroup(groupId: Int): List<QuestionEntity>

    @Query("SELECT * FROM question WHERE id = :id")
    suspend fun getQuestionById(id: Int): QuestionEntity?

    @Query("SELECT * FROM image")
    fun getAllImages(): List<ImageEntity>

    @Query("SELECT * FROM image WHERE id = :id")
    fun getAllImagesByID(id:Int): List<ImageEntity>

    @Transaction
    @Query("DELETE FROM form")
    suspend fun clearFormTable()

    @Transaction
    @Query("DELETE FROM question_group")
    suspend fun clearQuestionGroupTable()

    @Transaction
    @Query("DELETE FROM question")
    suspend fun clearQuestionTable()

    @Transaction
    suspend fun clearAll() {
        clearFormTable()
        clearQuestionGroupTable()
        clearQuestionTable()
    }
}