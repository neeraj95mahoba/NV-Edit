package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkQuality(val label: String, val shortLabel: String) {
    OFFLINE("Offline • Editor Ready", "Offline"),
    LOW_NETWORK("Weak Signal • Lite Mode", "Low Net"),
    GOOD_NETWORK("Online • High Speed", "Online")
}

data class NetworkStatus(
    val quality: NetworkQuality = NetworkQuality.GOOD_NETWORK,
    val isMetered: Boolean = false,
    val downstreamKbps: Int = 0,
    val manualLowDataMode: Boolean = false
) {
    val isOffline: Boolean get() = quality == NetworkQuality.OFFLINE
    val useLiteOptimization: Boolean get() = manualLowDataMode || quality == NetworkQuality.LOW_NETWORK
}

/**
 * Observes live Android network state & bandwidth estimates so NV Edit adapts
 * seamlessly on 2G/3G/weak-signal rural networks as well as full offline mode.
 */
class NetworkMonitor(context: Context, initialManualLowData: Boolean = false) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _status = MutableStateFlow(
        evaluateCurrentStatus(manualLowData = initialManualLowData)
    )
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refresh()
        }

        override fun onLost(network: Network) {
            refresh()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            refresh()
        }
    }

    init {
        registerCallback()
    }

    private fun registerCallback() {
        val cm = connectivityManager ?: return
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
            registered = true
        }
    }

    fun setManualLowDataMode(enabled: Boolean) {
        _status.value = evaluateCurrentStatus(manualLowData = enabled)
    }

    fun refresh() {
        val manual = _status.value.manualLowDataMode
        _status.value = evaluateCurrentStatus(manualLowData = manual)
    }

    fun stop() {
        if (registered) {
            runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
            registered = false
        }
    }

    private fun evaluateCurrentStatus(manualLowData: Boolean): NetworkStatus {
        val cm = connectivityManager ?: return NetworkStatus(
            quality = NetworkQuality.GOOD_NETWORK,
            manualLowDataMode = manualLowData
        )
        val activeNetwork = cm.activeNetwork
        val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null

        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkStatus(
                quality = NetworkQuality.OFFLINE,
                isMetered = false,
                downstreamKbps = 0,
                manualLowDataMode = manualLowData
            )
        }

        val downstreamKbps = caps.linkDownstreamBandwidthKbps
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isWifiOrEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        // Treat weak cellular links (< 1800 Kbps) or non-validated links as LOW_NETWORK
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val quality = when {
            !validated && downstreamKbps <= 0 -> NetworkQuality.LOW_NETWORK
            downstreamKbps in 1..1800 -> NetworkQuality.LOW_NETWORK
            isCellular && isMetered && downstreamKbps in 1..3000 -> NetworkQuality.LOW_NETWORK
            isWifiOrEthernet && downstreamKbps in 1..1200 -> NetworkQuality.LOW_NETWORK
            else -> NetworkQuality.GOOD_NETWORK
        }

        return NetworkStatus(
            quality = quality,
            isMetered = isMetered,
            downstreamKbps = downstreamKbps,
            manualLowDataMode = manualLowData
        )
    }
}
