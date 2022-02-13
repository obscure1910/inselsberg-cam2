package de.obscure.webcam.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import de.obscure.webcam.database.WeatherStatisticDatabase
import de.obscure.webcam.databinding.SettingsFragmentBinding
import de.obscure.webcam.viewmodels.SharedViewmodel
import de.obscure.webcam.viewmodels.SharedViewmodelFactory
import timber.log.Timber

class SettingsFragment : Fragment() {

    private val viewmodel: SharedViewmodel by activityViewModels {
        val application = requireActivity().application
        val statisticDao = WeatherStatisticDatabase.getInstance(application).weatherStatisticDatabaseDao
        SharedViewmodelFactory(statisticDao, application)
    }

    private lateinit var binding: SettingsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Timber.d("Settings view in creation phase")

        binding = SettingsFragmentBinding.inflate(inflater)

        binding.lifecycleOwner = this

        // Giving the binding access to the OverviewViewModel
        binding.viewmodel = viewmodel

        return binding.root
    }
}