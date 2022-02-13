package de.obscure.webcam.overview.photo

import de.obscure.webcam.network.InselsbergApi

class WebcamFragment : PhotoFragment() {

    override fun imageUrlGenerator(id: Long, size: String): String = InselsbergApi.imageUrlWebcam(id, size)

}