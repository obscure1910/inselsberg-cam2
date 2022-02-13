package de.obscure.webcam.fullscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.chrisbanes.photoview.PhotoView
import de.obscure.webcam.database.WeatherStatisticDatabase
import de.obscure.webcam.databinding.FullscreenFragmentBinding
import de.obscure.webcam.viewmodels.SharedViewmodel
import de.obscure.webcam.viewmodels.SharedViewmodelFactory
import timber.log.Timber

class FullscreenFragment : Fragment() {

    private val viewmodel: SharedViewmodel by activityViewModels {
        val application = requireActivity().application
        val statisticDao = WeatherStatisticDatabase.getInstance(application).weatherStatisticDatabaseDao
        SharedViewmodelFactory(statisticDao, application)
    }

    private lateinit var binding: FullscreenFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Timber.d("Fullscreen view in creation phase")

        setHasOptionsMenu(true)

        binding = FullscreenFragmentBinding.inflate(inflater)
        // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
        binding.lifecycleOwner = this

        // Giving the binding access to the OverviewViewModel
        binding.viewmodel = viewmodel

        val photoview = binding.photoView

        photoview.isZoomable = true
        photoview.setImageBitmap(viewmodel.shareableBitmap)

        return binding.root
    }
}