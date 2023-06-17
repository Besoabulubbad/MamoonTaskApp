    package com.example.mamoontaskapp.repository.database

    import android.content.Context
    import androidx.room.Database
    import androidx.room.Room
    import androidx.room.RoomDatabase

    @Database(entities = [FormEntity::class, QuestionGroupEntity::class, QuestionEntity::class , ImageEntity::class], version = 2, exportSchema = false)
    abstract class QuestionDatabase : RoomDatabase() {
        abstract fun questionDao(): QuestionDao

        companion object {
            @Volatile
            private var INSTANCE: QuestionDatabase? = null

            fun getDatabase(context: Context): QuestionDatabase {
                val tempInstance = INSTANCE
                if (tempInstance != null) {
                    return tempInstance
                }
                synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        QuestionDatabase::class.java,
                        "question_database"
                    )
                        // Added to handle migration from the old database version to the new one
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    return instance
                }
            }
        }
    }