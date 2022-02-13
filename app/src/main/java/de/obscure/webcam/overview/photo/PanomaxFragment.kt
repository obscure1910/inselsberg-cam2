package de.obscure.webcam.overview.photo

import de.obscure.webcam.network.InselsbergApi

class PanomaxFragment : PhotoFragment() {

    override fun imageUrlGenerator(id: Long, size: String): String = InselsbergApi.imageUrlPanomax(id, size)

}