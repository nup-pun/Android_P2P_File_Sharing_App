package com.xianfeng.wjcscx

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class NetworkService(private val context: Context) {

    private var nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    private val SERVICE_TYPE = "_http._tcp."
    private var serviceName = "FileTransferApp"
    private var role = ""

    var serverSocket: ServerSocket? = null
        private set
    private var localPort: Int = -1

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        initializeServerSocket()
    }

    private fun initializeServerSocket() {
        coroutineScope.launch {
            try {
                serverSocket = ServerSocket(0)
                localPort = serverSocket!!.localPort
                Log.d("NetworkService", "Server socket initialized on port: $localPort")
            } catch (e: IOException) {
                Log.e("NetworkService", "Error initializing server socket", e)
            }
        }
    }

    fun registerService(role: String) {
        this.role = role
        serviceName = "FileTransferApp_$role"

        if (registrationListener != null) return  // Already registered

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                Log.d("NetworkService", "Service registered: ${nsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NetworkService", "Service registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d("NetworkService", "Service unregistered: ${arg0.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NetworkService", "Service unregistration failed: $errorCode")
            }
        }

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = this@NetworkService.serviceName
            serviceType = SERVICE_TYPE
            port = localPort
        }
        coroutineScope.launch {
            // Wait for the server socket to be initialized before registering the service
            while (serverSocket == null) {
                delay(100)
            }
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }
    }

    fun startDiscovery(discoveryCallback: (NsdServiceInfo) -> Unit) {
        stopDiscovery() // Ensure any ongoing discovery is stopped before starting a new one

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("NetworkService", "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d("NetworkService", "Service discovery success: ${service.serviceName}")
                if (service.serviceType == SERVICE_TYPE && service.serviceName.startsWith("FileTransferApp_")) {
                    resolveService(service, discoveryCallback)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e("NetworkService", "Service lost: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("NetworkService", "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NetworkService", "Discovery failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NetworkService", "Discovery failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun resolveService(service: NsdServiceInfo, discoveryCallback: (NsdServiceInfo) -> Unit) {
        resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NetworkService", "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d("NetworkService", "Resolve Succeeded. $serviceInfo")
                discoveryCallback(serviceInfo)
            }
        }
        nsdManager.resolveService(service, resolveListener)
    }

    private fun stopDiscovery() {
        discoveryListener?.let {
            nsdManager.stopServiceDiscovery(it)
            discoveryListener = null
        }
    }

    fun stopService() {
        coroutineScope.launch {
            try {
                serverSocket?.close()
                registrationListener?.let {
                    nsdManager.unregisterService(it)
                    registrationListener = null
                }
                stopDiscovery()
            } catch (e: IOException) {
                Log.e("NetworkService", "Error stopping service", e)
            }
        }
    }

    fun connectToService(serviceInfo: NsdServiceInfo, connectionCallback: (Socket?) -> Unit) {
        coroutineScope.launch {
            try {
                delay(100) // Small delay to ensure server is ready
                val socket = Socket(serviceInfo.host, serviceInfo.port)
                withContext(Dispatchers.Main) {
                    connectionCallback(socket)
                }
                Log.d("NetworkService", "Connected to service: ${serviceInfo.serviceName} on port: ${serviceInfo.port}")
            } catch (e: IOException) {
                Log.e("NetworkService", "Failed to connect to service", e)
                withContext(Dispatchers.Main) {
                    connectionCallback(null)
                }
            }
        }
    }
}
