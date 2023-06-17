package com.example.mamoontaskapp.adapters

import android.content.ContentValues.TAG
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.mamoontaskapp.R
import com.example.mamoontaskapp.viewmodel.QuestionWithImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class SelectedPhotosAdapter(
    private val questionId: Int,
    val imagesFlow: Flow<List<QuestionWithImage>>
) : RecyclerView.Adapter<SelectedPhotosAdapter.ViewHolder>() {

    private var selectedPhotos: List<QuestionWithImage> = emptyList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var photoImageView: ImageView = itemView.findViewById(R.id.selected_photo_imageview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_photo, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photoUri = selectedPhotos[position].imageUri

        Glide.with(holder.itemView.context)
            .load(photoUri)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    e?.logRootCauses(TAG)
                    return false // important to return false so the error placeholder can be placed
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(holder.photoImageView)
    }

    override fun getItemCount(): Int {
        return selectedPhotos.size
    }

    init {
        CoroutineScope(Dispatchers.Main).launch {
            observeImagesFlow()
        }
    }

    private suspend fun observeImagesFlow() {
        imagesFlow
            .distinctUntilChanged()
            .mapLatest { images ->
                // Filter the images based on the questionId
                selectedPhotos = images.filter { it.questionId == questionId }
            }
            .collect {
                notifyDataSetChanged()
            }
    }
}