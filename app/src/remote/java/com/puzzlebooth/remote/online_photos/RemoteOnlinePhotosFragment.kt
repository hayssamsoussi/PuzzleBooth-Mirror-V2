package com.puzzlebooth.remote.online_photos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.puzzlebooth.main.base.BaseFragment
import com.puzzlebooth.main.qr_code.QRCodeFragment
import com.puzzlebooth.server.R
import com.puzzlebooth.server.databinding.FragmentAlbumBinding
import com.puzzlebooth.server.databinding.FragmentAlbumOnlineBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class RemoteOnlinePhotosFragment : BaseFragment<FragmentAlbumOnlineBinding>(R.layout.fragment_album_online) {

    override fun initViewBinding(view: View): FragmentAlbumOnlineBinding {
        return FragmentAlbumOnlineBinding.bind(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        service
            .listPhotos()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .doOnNext {
                val filtered = it.files
                    .filter { it.file.startsWith("thumb_") }
                    .sortedByDescending { it.file }
                    .map { "https://www.puzzleslb.com/puzzlebooth/uploads/mirror_booth_uploads/uploads/" + it.file }

                val adapter = AlbumAdapter(filtered) {
                    val index = it.indexOf("thumb_")
                    val fullUrl = it.removeRange(index, index+6)
                    val fragment = QRCodeFragment.newInstance(fullUrl)
                    fragment.show(parentFragmentManager, "")
                }

                binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
                binding.recyclerView.adapter = adapter
            }
            .subscribe()
    }
}
class AlbumAdapter(private val mList: List<String>, val action: (String) -> Unit) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.file_item, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val file = mList[position]

        holder.itemView.setOnClickListener {
            action.invoke(file)
        }

        Glide.with(holder.itemView.context)
            .load(file)
//            .transform(
//                RotateTransformation(
//                    holder.itemView.context,
//                    270f
//                )
//            )
            .into(holder.imageView)
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageview)
    }
}