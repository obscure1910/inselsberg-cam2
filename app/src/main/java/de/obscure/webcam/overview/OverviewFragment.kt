package de.obscure.webcam.overview

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import de.obscure.webcam.R
import de.obscure.webcam.database.WeatherStatisticDatabase
import de.obscure.webcam.databinding.OverviewFragmentBinding
import de.obscure.webcam.overview.photo.PhotoSlideAdapter
import de.obscure.webcam.viewmodels.SharedViewmodel
import de.obscure.webcam.viewmodels.SharedViewmodelFactory
import timber.log.Timber

class OverviewFragment : Fragment() {

    private val viewmodel: SharedViewmodel by activityViewModels {
        val application = requireActivity().application
        val statisticDao = WeatherStatisticDatabase.getInstance(application).weatherStatisticDatabaseDao
        SharedViewmodelFactory(statisticDao, application)
    }

    private lateinit var viewPager2: ViewPager2
    private lateinit var binding: OverviewFragmentBinding
    private var currentPosition: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Timber.d("Overview view in creation phase")

        setHasOptionsMenu(true)

        binding = OverviewFragmentBinding.inflate(inflater)
        // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
        binding.lifecycleOwner = this

        // Giving the binding access to the OverviewViewModel
        binding.viewmodel = viewmodel

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar?.setDisplayShowTitleEnabled(false)

        //view pager to show both webcam images
        val photoSlideAdapter = PhotoSlideAdapter(this)
        viewPager2 = binding.pagerImage
        viewPager2.adapter = photoSlideAdapter

        currentPosition = savedInstanceState?.getInt(CURRENT_POSITION)

        currentPosition?.let { viewPager2.setCurrentItem(it, false) }

        viewmodel.preferredPhoto.observe(viewLifecycleOwner, { preferredPhoto ->
            Timber.d("\r\n Observer PreferredPhoto - $preferredPhoto \r\n")
            if (currentPosition == null) {
                currentPosition = preferredPhoto.id
                viewPager2.setCurrentItem(preferredPhoto.id, false)
            }
        })

        TabLayoutMediator(binding.tabs, viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = "Skihang"
                1 -> tab.text = "Panorama"
                2 -> tab.text = "Bad Tabarz"
            }
        }.attach()

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentPosition?.let { outState.putInt(CURRENT_POSITION, it) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> findNavController().navigate(OverviewFragmentDirections.actionOverviewToSettingsFragment())
            R.id.share -> viewmodel.shareBitmap(requireActivity())
        }
        return true
    }

    companion object {
        const val CURRENT_POSITION = "viewpager.current.position"
    }
}