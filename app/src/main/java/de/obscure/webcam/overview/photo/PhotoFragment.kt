package de.obscure.webcam.overview.photo

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import de.obscure.webcam.DateTimeExtensions.forGermany
import de.obscure.webcam.DateTimeExtensions.printGermanLongFormat
import de.obscure.webcam.databinding.PhotoFragmentBinding
import de.obscure.webcam.overview.OverviewFragmentDirections
import de.obscure.webcam.viewmodels.NetworkConnectionType
import de.obscure.webcam.viewmodels.SharedViewmodel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import timber.log.Timber


abstract class PhotoFragment : Fragment() {

    private var job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    lateinit var viewmodel: SharedViewmodel
    private lateinit var binding: PhotoFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = PhotoFragmentBinding.inflate(inflater)
        // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
        binding.lifecycleOwner = requireActivity()

        viewmodel = activityViewModels<SharedViewmodel>().value

        binding.photoImage.setOnClickListener {
            findNavController().navigate(OverviewFragmentDirections.actionOverviewToFullscreenFragment())
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewmodel.visibleEntry.observe(viewLifecycleOwner, {
            uiScope.launch {
                when (viewmodel.requestNetworkConnectionType()) {
                    NetworkConnectionType.Slow -> {
                        loadInto(binding.photoImage, it.id, "small")
                    }
                    NetworkConnectionType.Fast -> {
                        loadInto(binding.photoImage, it.id, "normal")
                    }
                    else -> null
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        binding.unbind()
        viewmodel.visibleEntry.removeObservers(viewLifecycleOwner)
    }

    abstract fun imageUrlGenerator(id: Long, size: String): String

    private fun loadInto(view: ImageView, id: Long, pictureSize: String) {
        val url = imageUrlGenerator(id, pictureSize)
        Timber.d(
            "Load image: \r\n id:\t\t %s \r\n quality:\t %s \r\n url:\t\t %s",
            id,
            pictureSize,
            url
        )
        val circularProgressDrawable = CircularProgressDrawable(view.context).apply {
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }

        GlideApp.with(view.context)
            .asBitmap()
            .load(url)
            .apply(
                RequestOptions()
                    .optionalFitCenter()
                        // use better quality
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .override(Target.SIZE_ORIGINAL))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .signature(ObjectKey(url))
            .placeholder(circularProgressDrawable)

            .listener(object: RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    resource?.let {
                        viewmodel.setCurrentVisibleBitmap(it)
                    }
                    return false
                }
            })
            .into(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}