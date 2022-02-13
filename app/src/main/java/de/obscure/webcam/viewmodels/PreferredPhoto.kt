package de.obscure.webcam.viewmodels

enum class PreferredPhoto(val id: Int) {

    SKIHANG(0),
    PANORAMA(1),
    BAD_TABARZ(2);

    companion object {

        const val SHAREDPREFERENCES_KEY = "de.obscure.webcam.viewmodels.PreferredPhoto"

        fun byId(id: Int) : PreferredPhoto =
            when(id) {
                0 -> SKIHANG
                1 -> PANORAMA
                2 -> BAD_TABARZ
                else -> SKIHANG
            }
    }

}