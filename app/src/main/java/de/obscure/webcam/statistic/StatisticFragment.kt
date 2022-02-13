package de.obscure.webcam.statistic

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import de.obscure.webcam.DateTimeExtensions.forGermany
import de.obscure.webcam.DateTimeExtensions.printGermanDayMonthFormat
import de.obscure.webcam.R
import de.obscure.webcam.database.WeatherStatisticDatabase
import de.obscure.webcam.databinding.StatisticFragmentBinding
import de.obscure.webcam.viewmodels.SharedViewmodel
import de.obscure.webcam.viewmodels.SharedViewmodelFactory
import org.joda.time.DateTime
import timber.log.Timber

class StatisticFragment : Fragment() {

    private val viewmodel: SharedViewmodel by activityViewModels {
        val application = requireActivity().application
        val statisticDao = WeatherStatisticDatabase.getInstance(application).weatherStatisticDatabaseDao
        SharedViewmodelFactory(statisticDao, application)
    }

    private lateinit var binding: StatisticFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Timber.d("Statistic view in creation phase")

        binding = StatisticFragmentBinding.inflate(inflater)
        // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("Statistic view created")

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar?.setDisplayShowTitleEnabled(false)

        viewmodel.chartEntries.observe(viewLifecycleOwner, { chartDataEntries ->

            if(!chartDataEntries.dataTemperature.isNullOrEmpty()) {
                val chart = binding.chart
                val chartValueSelectedListener = ChartValueSelected(this, viewmodel)
                val markerView = CustomMarkerView(activity, R.layout.marker_view)
                val maxRain = chartDataEntries.dataRain.maxOfOrNull { re -> re.rain } ?: 0.0f

                val dataSetTemperature = LineDataSet(chartDataEntries.dataTemperature, "Temperatur (°C)").apply {
                    color = ContextCompat.getColor(activity, R.color.statisticTemperature)
                    lineWidth = 2f
                    axisDependency = YAxis.AxisDependency.LEFT
                    isHighlightEnabled = true
                    highLightColor = Color.BLACK
                    setDrawHighlightIndicators(true)
                    setDrawCircles(false)
                }

                val dataSetRain = LineDataSet(chartDataEntries.dataRain, "Niederschlag (l/m²)").apply {
                    axisDependency = YAxis.AxisDependency.RIGHT
                    isHighlightEnabled = false
                    color = ContextCompat.getColor(activity, R.color.statisticRain)
                    lineWidth = 2f
                    setDrawCircles(false)
                }

                // show vertical line at midnight every day
                generateSequence(midnight(chartDataEntries.dataTemperature.first().milliseconds), StatisticFragment::plusOneDay)
                    .take(6)
                    .forEach { milliseconds ->
                        with(LimitLine(milliseconds.toFloat(), DateTime(milliseconds).forGermany().printGermanDayMonthFormat())) {
                            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                            textColor = ContextCompat.getColor(activity, R.color.mainText)
                            enableDashedLine(5f, 5f, 0f)
                            lineColor = Color.GRAY
                            chart.xAxis.addLimitLine(this)
                        }
                    }

                with(chart) {
                    data = LineData(dataSetTemperature, dataSetRain)
                    axisLeft.textColor = dataSetTemperature.color
                    axisRight.textColor = dataSetRain.color
                    //Rain can not be less than zero
                    axisRight.axisMinimum = 0.0f
                    //scale y-axis on the right side
                    axisRight.axisMaximum = when (maxRain) {
                        in 0.00f..2.99f -> 4.00f
                        in 2.99f..4.99f -> 6.00f
                        else -> maxRain + 1.00f
                    }
                    if (chartDataEntries.showZeroLine) {
                        axisLeft.setDrawZeroLine(true)
                        axisLeft.zeroLineColor = Color.RED
                    }

                    legend.textColor = ContextCompat.getColor(activity, R.color.mainText)
                    marker = markerView
                    //do not show standard labels on the x axis
                    xAxis.setDrawLabels(false)
                    xAxis.setDrawGridLines(false)
                    onChartGestureListener = chartValueSelectedListener
                    setOnChartValueSelectedListener(chartValueSelectedListener)
                    //disable text in left bottom corner
                    description.isEnabled = false
                    //disable zoom
                    setScaleEnabled(false)
                    //refresh chart
                    invalidate()
                }
            }


        })

        // Giving the binding access to the OverviewViewModel
        binding.viewmodel = viewmodel
    }

    class ChartValueSelected(
        private val fragment: Fragment,
        private val viewmodel: SharedViewmodel
    ) : OnChartValueSelectedListener, OnChartGestureListener {

        private var _chartSelectedValue: TemperatureEntry? = null

        override fun onValueSelected(e: Entry?, h: Highlight?) {
            if (e is TemperatureEntry) {
                _chartSelectedValue = e
            }
        }

        override fun onNothingSelected() {
            _chartSelectedValue = null
        }

        override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {
            _chartSelectedValue?.let {
                viewmodel.onValueSelected(it)
                fragment.findNavController().navigate(StatisticFragmentDirections.actionStatisticToOverview())
            }
        }

        override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {
            _chartSelectedValue = null
            viewmodel.onNothingSelected()
        }

        override fun onChartLongPressed(me: MotionEvent?) {}

        override fun onChartDoubleTapped(me: MotionEvent?) {}

        override fun onChartSingleTapped(me: MotionEvent?) {}

        override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}

        override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}

        override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}

    }


    companion object {
        fun plusOneDay(milliseconds: Long): Long {
            return DateTime(milliseconds).plusDays(1).millis
        }

        fun midnight(milliseconds: Long): Long {
            return DateTime(milliseconds).forGermany().withTime(0, 0, 0, 0).millis
        }
    }

}