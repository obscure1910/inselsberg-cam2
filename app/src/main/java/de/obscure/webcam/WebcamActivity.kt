package de.obscure.webcam

import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.obscure.webcam.database.WeatherStatisticDatabase
import de.obscure.webcam.network.InselsbergApi
import de.obscure.webcam.repositories.WeatherStatisticRepository
import de.obscure.webcam.viewmodels.SharedViewmodel
import de.obscure.webcam.viewmodels.SharedViewmodelFactory
import kotlinx.coroutines.*


class WebcamActivity : AppCompatActivity() {

    private val viewmodel: SharedViewmodel by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val statisticDao = WeatherStatisticDatabase.getInstance(application).weatherStatisticDatabaseDao
        val viewModelFactory = SharedViewmodelFactory(statisticDao, application)
        ViewModelProvider(this, viewModelFactory)[SharedViewmodel::class.java]
    }

    private var job: Job? = null

    private val repository: WeatherStatisticRepository by lazy(LazyThreadSafetyMode.PUBLICATION) {
        WeatherStatisticRepository(
            viewmodel,
            WeatherStatisticDatabase.getInstance(application).weatherStatisticDatabaseDao,
            InselsbergApi.retrofitService
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupBottomNavigationView()
        setupDialog()
    }

    private fun setupDialog() {
        if(viewmodel.isDialogVisible()) {
            val dialog = layoutInflater.inflate(R.layout.dialog, null)
            val builder = AlertDialog.Builder(this)
            builder
                .setView(dialog)
                .setPositiveButton("OK") { _, _ ->
                    val cb = dialog.findViewById<CheckBox>(R.id.dialog_skip)
                    if (cb.isChecked) {
                        viewmodel.hideDialog()
                    }
                }
                .show()
        }
    }

    private fun setupBottomNavigationView() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        viewmodel.onActivityCreate()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            viewmodel.onNavigation(destination.id)
        }
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val overviewItem = bottomNavigationView.menu.findItem(R.id.overview)
        viewmodel.overviewIcon.observe(this) {
            overviewItem.icon = it
        }
        bottomNavigationView.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        if(job?.isActive == true) {
            job?.cancel()
        }
        job = GlobalScope.launch(Dispatchers.IO) {
            repository.refreshWeatherStatistics()
        }
        job?.invokeOnCompletion { throwable ->
            throwable?.let {
                FirebaseCrashlytics.getInstance().recordException(it)
                FirebaseCrashlytics.getInstance().sendUnsentReports()
            }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

}