package de.obscure.webcam.statistic

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.DisplayMetrics
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import de.obscure.webcam.DateTimeExtensions.forGermany
import de.obscure.webcam.DateTimeExtensions.printGermanLongFormat
import de.obscure.webcam.R
import org.joda.time.DateTime

@SuppressLint("ViewConstructor")
class CustomMarkerView(
    context: Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    private val tvSelectedDataPoint: TextView by lazy { findViewById(R.id.tvSelectedDataPoint) }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)
        if (e is TemperatureEntry) {
            tvSelectedDataPoint.text = formatMarkerViewText(context.resources, e)
        }
    }

    // draw marker view to the top left corner
    override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
        val x = convertDpToPixel(20f, context)
        val y = convertDpToPixel(20f, context)

        canvas?.apply {
            translate(x, y)
            draw(canvas)
            canvas.translate(-x, -y)
        }
    }

    companion object {

        fun formatMarkerViewText(resources: Resources, temperatureEntry: TemperatureEntry): Spanned {
            val sb = StringBuilder()
            sb.apply {
                append("${resources.getString(R.string.time)} ${DateTime(temperatureEntry.milliseconds).forGermany().printGermanLongFormat()}<br>")
                append("${resources.getString(R.string.temperature)} ${temperatureEntry.temperature} °C<br>")
                append("${resources.getString(R.string.rain)} ${temperatureEntry.rain} l/m²")
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
            } else {
                HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }

        fun convertDpToPixel(dp: Float, context: Context): Float {
            return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }
    }

}