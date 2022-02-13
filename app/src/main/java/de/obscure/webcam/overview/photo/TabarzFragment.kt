package de.obscure.webcam.overview.photo

import de.obscure.webcam.network.InselsbergApi

class TabarzFragment : PhotoFragment() {

    override fun imageUrlGenerator(id: Long, size: String): String =  InselsbergApi.imageUrlTabarz(id, size)
}