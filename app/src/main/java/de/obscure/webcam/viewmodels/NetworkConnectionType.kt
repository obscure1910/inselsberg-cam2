package de.obscure.webcam.viewmodels

sealed class NetworkConnectionType {

    object Fast: NetworkConnectionType()
    object Slow: NetworkConnectionType()
    object Unavailable: NetworkConnectionType()

}

