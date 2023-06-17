package com.example.mamoontaskapp.adapters

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mamoontaskapp.R
import com.example.mamoontaskapp.model.QuestionDataBase
import com.example.mamoontaskapp.repository.database.Answer
import com.example.mamoontaskapp.viewmodel.QuestionWithImage
import com.example.mamoontaskapp.viewmodel.QuestionsViewModel
import kotlinx.coroutines.flow.Flow

class QuestionAdapter(
    var questions: List<QuestionDataBase>,
    private val viewModel: QuestionsViewModel,
    private val imagesFlow: Flow<List<QuestionWithImage>>
) : RecyclerView.Adapter<QuestionAdapter.ViewHolder>() {

    interface GalleryButtonListener {
        fun onGalleryButtonClick(question: QuestionDataBase, adapter: QuestionAdapter)
    }

    private var galleryButtonListener: GalleryButtonListener? = null

    fun setGalleryButtonListener(listener: GalleryButtonListener) {
        galleryButtonListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]

        // Set the question text and description
        holder.questionTextView.text = question.text
        holder.descriptionEditText.setText(question.description)

        // Set up the selected photos adapter for the question
        val selectedPhotosAdapter = SelectedPhotosAdapter(questionId = question.id, imagesFlow)
        holder.selectedPhotosRecyclerView.adapter = selectedPhotosAdapter
        holder.selectedPhotosRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)

        // Set the button backgrounds based on the answer
        changeBackgroundOnLoad( holder, question)

        // Handle expand/collapse functionality
        if (question.isExpanded && !holder.expandCollapseAnimator.isExpanded) {
            holder.expandCollapseAnimator.expand()
        } else if (!question.isExpanded && holder.expandCollapseAnimator.isExpanded) {
            holder.expandCollapseAnimator.collapse()
        }

        // Handle click events
        holder.questionTextView.setOnClickListener {
            if (question.isExpanded) {
                holder.expandCollapseAnimator.collapse()
                viewModel.onExpandChange(question, isExpanded = false)
                question.isExpanded = false
            } else {
                holder.expandCollapseAnimator.expand()
                viewModel.onExpandChange(question, isExpanded = true)
                question.isExpanded = true
            }
        }

        holder.noTextView.setOnClickListener {
            handleButtonClick(holder, it as TextView, question)
        }

        holder.yesTextView.setOnClickListener {
            handleButtonClick(holder, it as TextView, question)
        }

        holder.descriptionEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.onDescriptionChange(question, s.toString())
                question.description = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        holder.galleryButton.setOnClickListener {
            val questionForGallery = questions[holder.adapterPosition]
            galleryButtonListener?.onGalleryButtonClick(questionForGallery, this)
        }

        // Update the selected photos adapter for the question
        selectedPhotosAdapter.notifyItemChanged(position)
    }

    private fun changeBackgroundOnLoad( holder: ViewHolder, question: QuestionDataBase) {
        when (question.answer) {
            Answer.YES -> {
                holder.yesTextView.setBackgroundResource(R.drawable.yes_background)
                holder.noTextView.setBackgroundResource(0)
            }
            Answer.NO -> {
                holder.noTextView.setBackgroundResource(R.drawable.no_background)
                holder.yesTextView.setBackgroundResource(0)
            }
            else -> {
                holder.yesTextView.setBackgroundResource(0)
                holder.noTextView.setBackgroundResource(0)
            }
        }
    }

    private fun handleButtonClick(holder: ViewHolder, button: TextView, question: QuestionDataBase) {
        if (question.answer == Answer.YES && button.id == R.id.yes_textview) {
            resetButton(holder, question)
        } else if (question.answer == Answer.NO && button.id == R.id.no_textview) {
            resetButton(holder, question)
        } else {
            selectButton(button, question, holder)
        }

        viewModel.onAnswerChange(question, question.answer)
    }

    private fun selectButton(button: TextView, question: QuestionDataBase, holder: ViewHolder) {
        // Reset both buttons to default
        resetButtonColor(holder.yesTextView)
        resetButtonColor(holder.noTextView)

        // Highlight the selected button
        button.setTextColor(Color.WHITE)
        button.setBackgroundResource(if (button.id == R.id.yes_textview) R.drawable.yes_background else R.drawable.no_background)

        // Update the answer
        question.answer = if (button.id == R.id.yes_textview) Answer.YES else Answer.NO

        if (button.id == R.id.no_textview && !holder.expandCollapseAnimator.isExpanded) {
            holder.expandCollapseAnimator.expand()
            viewModel.onExpandChange(question, true)
        } else {
            holder.expandCollapseAnimator.collapse()
            viewModel.onExpandChange(question, false)
        }
    }

    private fun resetButton(holder: ViewHolder, question: QuestionDataBase) {
        resetButtonColor(holder.yesTextView)
        resetButtonColor(holder.noTextView)
        question.answer = Answer.UNANSWERED
        holder.expandCollapseAnimator.collapse()
        viewModel.onExpandChange(question, false)
    }

    private fun resetButtonColor(button: TextView) {
        button.setTextColor(Color.BLACK)
        button.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun getItemCount(): Int {
        return questions.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val descriptionEditText: EditText = itemView.findViewById(R.id.description_edittext)
        val questionTextView: TextView = itemView.findViewById(R.id.question_text)
        private val expandedView: View = itemView.findViewById(R.id.expanded_view)
        val yesTextView: TextView = itemView.findViewById(R.id.yes_textview)
        val noTextView: TextView = itemView.findViewById(R.id.no_textview)
        val expandCollapseAnimator: ExpandCollapseAnimator = ExpandCollapseAnimator(expandedView)
        val galleryButton: ImageButton = itemView.findViewById(R.id.gallery_button)
        val selectedPhotosRecyclerView: RecyclerView = itemView.findViewById(R.id.selected_photos_recyclerview)
    }
}