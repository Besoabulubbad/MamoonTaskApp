    package com.example.mamoontaskapp.adapters

    import android.content.ContentValues.TAG
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.TextView
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.example.mamoontaskapp.R
    import com.example.mamoontaskapp.model.QuestionsDataBaseGroup
    import com.example.mamoontaskapp.viewmodel.QuestionWithImage
    import com.example.mamoontaskapp.viewmodel.QuestionsViewModel
    import kotlinx.coroutines.flow.Flow


    class QuestionGroupAdapter(
        private val viewModel: QuestionsViewModel,
        private val galleryButtonListener: QuestionAdapter.GalleryButtonListener
    ) : RecyclerView.Adapter<QuestionGroupAdapter.ViewHolder>() {

        private var questionGroups: List<QuestionsDataBaseGroup>? = null
        private var imagesFlow: Flow<List<QuestionWithImage>>? = null
        private var loadedQuestionGroupCount = 0

        // ViewHolder class
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val questionGroupTitleTextView: TextView = itemView.findViewById(R.id.question_group_title)
            val questionCountTextView: TextView = itemView.findViewById(R.id.question_count)
            val questionRecyclerView: RecyclerView = itemView.findViewById(R.id.question_recycler_view)
            lateinit var questionAdapter: QuestionAdapter
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Inflate the item layout for each question group
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.question_group_layout, parent, false)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val questionGroup = questionGroups?.get(position)
            questionGroup?.let {
                // Set the title and question count for the question group
                holder.questionGroupTitleTextView.text = questionGroup.name
                holder.questionCountTextView.text = questionGroup.questions.size.toString()

                // Create an instance of the QuestionAdapter and set it as the adapter for the question RecyclerView
                val questionAdapter = QuestionAdapter(questionGroup.questions, viewModel, imagesFlow!!)
                questionAdapter.setGalleryButtonListener(galleryButtonListener)
                holder.questionAdapter = questionAdapter
                holder.questionRecyclerView.adapter = questionAdapter

                // Set the visibility and layout manager for the question RecyclerView based on the expanded state
                if (position < loadedQuestionGroupCount) {
                    holder.questionRecyclerView.visibility = View.VISIBLE
                    holder.questionRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
                } else {
                    holder.questionRecyclerView.visibility = View.GONE
                }

                // Load more question groups if this is the last item
                if (position == itemCount - 1) {
                    loadMoreQuestionGroups()
                }
            }
        }

        override fun getItemCount(): Int {
            // Return the minimum of loaded question groups and the actual number of question groups
            return loadedQuestionGroupCount.coerceAtMost(questionGroups?.size ?: 0)
        }

        /**
         * Set the question groups and images for the adapter.
         * This method updates the question groups and triggers a UI update.
         */
        fun setQuestionGroupsAndImages(questionGroups: List<QuestionsDataBaseGroup>, image: Flow<List<QuestionWithImage>>) {
            val prevSize = itemCount
            this.questionGroups = questionGroups
            this.imagesFlow = image
            loadedQuestionGroupCount = 5.coerceAtMost(questionGroups.size)
            val newSize = itemCount
            notifyItemRangeInserted(prevSize, newSize - prevSize)
        }

        /**
         * Load more question groups if available.
         * This method loads additional question groups in batches of 5.
         */
        private fun loadMoreQuestionGroups() {
            val remainingQuestionGroups = (questionGroups?.size ?: 0) - loadedQuestionGroupCount
            val groupsToLoad = if (remainingQuestionGroups >= 5) 5 else remainingQuestionGroups
            if (groupsToLoad > 0) {
                val prevCount = loadedQuestionGroupCount
                loadedQuestionGroupCount += groupsToLoad

                // Notify the adapter about the inserted range of question groups
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post {
                    notifyItemRangeInserted(prevCount, groupsToLoad)
                }

                Log.d("QuestionGroupAdapter", "Loaded additional $groupsToLoad question groups")
                // Invoke the listener if needed
            } else {
                Log.d("QuestionGroupAdapter", "No more question groups to load")
            }
        }

        /**
         * Get the QuestionAdapter for a specific question ID.
         * This method searches for the adapter associated with the given question ID.
         */
        fun getQuestionAdapter(recyclerView: RecyclerView, questionId: Int): QuestionAdapter? {
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                val viewHolder = recyclerView.getChildViewHolder(child) as? ViewHolder
                viewHolder?.let { holder ->
                    val questionAdapter = holder.questionAdapter

                    if (questionAdapter.questions.any { it.id == questionId }) {
                        Log.d(TAG, "Found adapter for questionId: $questionId")
                        return questionAdapter
                    }
                }
            }

            Log.e(TAG, "Adapter not found for questionId: $questionId")
            return null
        }
    }