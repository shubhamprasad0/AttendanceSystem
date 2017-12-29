package com.example.shubham.attendancesystemteacherversion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.Locale

abstract class AttendanceActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private val TAG = "Teacher App"

    /**
     * These permissions are required before connecting to Nearby Connections. Only {@link
     * Manifest.permission#ACCESS_COARSE_LOCATION} is considered dangerous, so the others should be
     * granted just by having them in our AndroidManifest.xml
     */
    private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
     */
    private val STRATEGY = Strategy.P2P_STAR

    /** We'll talk to Nearby Connections through the GoogleApiClient. */
    private var googleApiClient: GoogleApiClient? = null

    /** The devices we've discovered near us.  */
    private val discoveredEndpoints = HashMap<String, Endpoint>()

    /**
     * The devices we have pending connections to. They will stay pending until we call [ ][.acceptConnection] or [.rejectConnection].
     */
    private val pendingConnections = HashMap<String, Endpoint>()

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     */
    private val establishedConnections = HashMap<String, Endpoint>()

    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * device.
     */
    private var isConnecting = false

    /** True if we are discovering.  */
    private var isDiscovering = false

    /** True if we are advertising.  */
    private var isAdvertising = false

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object: ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            logD("onConnectionInitiated(endpointId=$endpointId, endpointName=${connectionInfo.endpointName}")
            val endpoint = Endpoint(endpointId, connectionInfo.endpointName)
            pendingConnections.put(endpointId, endpoint)
            this@AttendanceActivity.onConnectionInitiated(endpoint, connectionInfo)
        }


        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            logD("onConnectionResponse(endpointId=$endpointId, result=$result")
            isConnecting = false

            if (!result.status.isSuccess) {
                logW("Connection failed. Received status ${result.status}")
                onConnectionFailed(pendingConnections.remove(endpointId))
                return
            }
            connectedToEndpoint(pendingConnections.remove(endpointId))
        }

        override fun onDisconnected(endpointId: String) {
            if (!establishedConnections.containsKey(endpointId)) {
                logW("Unexpected disconnection from endpoint $endpointId")
                return
            }
            disconnectedFromEndpoint(establishedConnections[endpointId])
        }
    }

    private val payloadCallback = object: PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            logD("onPayloadReceived(endpointId=$endpointId, payload=$payload")
            onReceive(establishedConnections[endpointId], payload)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            logD("onPayloadTransferUpdate(endpointId=$endpointId, update=$update")
        }
    }

    private fun resetState() {
        discoveredEndpoints.clear();
        pendingConnections.clear();
        establishedConnections.clear();
        isConnecting = false;
        isDiscovering = false;
        isAdvertising = false;
    }

    private fun createGoogleApiClient() {
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(this)
                    .addApi(Nearby.CONNECTIONS_API)
                    .addConnectionCallbacks(this)
                    .enableAutoManage(this, this)
                    .build()
        }
    }

    /**
     * Our Activity has just been made visible to the user. Our GoogleApiClient will start connecting
     * after super.onStart() is called.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        if (hasPermissions(this, getRequiredPermissions())) {
            createGoogleApiClient()
        } else {
            requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS)
        }
        super.onStart()
    }

    /** We've connected to Nearby Connections' GoogleApiClient.  */
    override fun onConnected(bundle: Bundle?) {
        logV("onConnected")
    }

    /** We've been temporarily disconnected from Nearby Connections' GoogleApiClient.  */
    @CallSuper
    override fun onConnectionSuspended(reason: Int) {
        logW(String.format("onConnectionSuspended(reason=%s)", reason))
        resetState()
    }

    /** We are unable to connect to Nearby Connections' GoogleApiClient. Oh uh.  */
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        logW(
                String.format(
                        "onConnectionFailed(%s)",
                        toString(Status(connectionResult.errorCode))))
    }


    /** The user has accepted (or denied) our permission request.  */
    @CallSuper
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            for (grantResult in grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
            }
            recreate()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    /**
     * Sets the device to advertising mode. It will broadcast to other devices in discovery mode.
     * Either [.onAdvertisingStarted] or [.onAdvertisingFailed] will be called once
     * we've found out if we successfully entered this mode.
     */
     /**
     * Sets the device to advertising mode. It will broadcast to other devices in discovery mode.
     * Either [.onAdvertisingStarted] or [.onAdvertisingFailed] will be called once
     * we've found out if we successfully entered this mode.
     */
    protected fun startAdvertising() {
     isConnecting = true
     Nearby.Connections.startAdvertising(
             googleApiClient,
             getName(),
             getServiceId(),
             connectionLifecycleCallback,
             AdvertisingOptions(STRATEGY))
             .setResultCallback { result ->
                 if (result.status.isSuccess) {
                     logV("Now advertising endpoint " + result.localEndpointName)
                     onAdvertisingStarted()
                 } else {
                     isAdvertising = false
                     logW(
                             String.format(
                                     "Advertising failed. Received status %s.",
                                     AttendanceActivity.toString(result.status)))
                     onAdvertisingFailed()
                 }
             }
    }

    /** Stops advertising.  */
    protected fun stopAdvertising() {
        isAdvertising = false
        Nearby.Connections.stopAdvertising(googleApiClient)
    }

    /** @return True if currently advertising.
     */
    protected fun isAdvertising(): Boolean {
        return isAdvertising
    }

    /** Advertising has successfully started. Override this method to act on the event.  */
    protected fun onAdvertisingStarted() {}

    /** Advertising has failed to start. Override this method to act on the event.  */
    protected fun onAdvertisingFailed() {}

    /**
     * A pending connection with a remote endpoint has been created. Use [ConnectionInfo] for
     * metadata about the connection (like incoming vs outgoing, or the authentication token). If we
     * want to continue with the connection, call [.acceptConnection]. Otherwise, call
     * [.rejectConnection].
     */
    protected fun onConnectionInitiated(endpoint: Endpoint, connectionInfo: ConnectionInfo) {}

    /** Accepts a connection request.  */
    protected fun acceptConnection(endpoint:Endpoint) {
        Nearby.Connections.acceptConnection(googleApiClient, endpoint.id, payloadCallback)
                .setResultCallback { status ->
                    if (!status.isSuccess) {
                        logW(
                                String.format(
                                        "acceptConnection failed. %s", toString(status)))
                    }
                }
    }


    /** Rejects a connection request.  */
    protected fun rejectConnection(endpoint:Endpoint) {
        Nearby.Connections.rejectConnection(googleApiClient, endpoint.id)
                .setResultCallback { status ->
                    if (!status.isSuccess) {
                        logW(
                                String.format(
                                        "rejectConnection failed. %s", toString(status)))
                    }
                }
    }

    /**
   * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
   * {@link #onDiscoveryStarted()} ()} or {@link #onDiscoveryFailed()} ()} will be called once we've
   * found out if we successfully entered this mode.
   */
   protected fun startDiscovering() {
        isDiscovering = true
        discoveredEndpoints.clear()
        Nearby.Connections.startDiscovery(
                googleApiClient,
                getServiceId(),
                object: EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        logD(
                                String.format(
                                        "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                        endpointId, info.serviceId, info.endpointName))

                        if (getServiceId() == info.serviceId) {
                            val endpoint = Endpoint(endpointId, info.endpointName)
                            discoveredEndpoints.put(endpointId, endpoint)
                            onEndpointDiscovered(endpoint)
                        }
                    }

                    override public fun onEndpointLost(endpointId: String) {
                        logD(String.format("onEndpointLost(endpointId=%s)", endpointId))
                    }
                },
                DiscoveryOptions(STRATEGY))
                .setResultCallback(
                        ResultCallback<Status> {
                            fun onResult(status: Status) {
                                if (status.isSuccess) {
                                    onDiscoveryStarted()
                                } else {
                                    isDiscovering = false
                                    logW(
                                            String.format(
                                                    "Discovering failed. Received status %s.",
                                                    toString(status)))
                                    onDiscoveryFailed()
                        }

                }
              }
            )
  }

    /** Stops discovery.  */
    protected fun stopDiscovering() {
        isDiscovering = false
        Nearby.Connections.stopDiscovery(googleApiClient)
    }

    /** @return True if currently discovering.
     */
    protected fun isDiscovering(): Boolean {
        return isDiscovering
    }

    /** Discovery has successfully started. Override this method to act on the event.  */
    protected fun onDiscoveryStarted() {}

    /** Discovery has failed to start. Override this method to act on the event.  */
    protected fun onDiscoveryFailed() {}

    /**
     * A remote endpoint has been discovered. Override this method to act on the event. To connect to
     * the device, call [.connectToEndpoint].
     */
    protected fun onEndpointDiscovered(endpoint: Endpoint) {}

    protected fun disconnect(endpoint: Endpoint) {
        Nearby.Connections.disconnectFromEndpoint(googleApiClient, endpoint.id)
        establishedConnections.remove(endpoint.id)
    }

    protected fun disconnectFromAllEndpoints() {
        for (endpoint in establishedConnections.values) {
          Nearby.Connections.disconnectFromEndpoint(googleApiClient, endpoint.id)
        }
        establishedConnections.clear()
    }

    /** Sends a connection request to the endpoint.  */
    protected fun connectToEndpoint(endpoint:Endpoint) {
        // If we already sent out a connection request, wait for it to return
        // before we do anything else. P2P_STAR only allows 1 outgoing connection.
        if (isConnecting) {
            logW("Already connecting, so ignoring this endpoint: " + endpoint)
            return
        }

        logV("Sending a connection request to endpoint " + endpoint)

        // Mark ourselves as connecting so we don't connect multiple times
        isConnecting = true

        // Ask to connect
        Nearby.Connections.requestConnection(
                googleApiClient, getName(), endpoint.id, connectionLifecycleCallback)
                .setResultCallback { status ->
                    if (!status.isSuccess) {
                        logW(
                                String.format(
                                        "requestConnection failed. %s", toString(status)))
                        isConnecting = false
                        onConnectionFailed(endpoint)
                    }
                }
    }

    /** True if we're currently attempting to connect to another device.  */
    protected fun isConnecting(): Boolean {
        return isConnecting
    }


    private fun connectedToEndpoint(endpoint: Endpoint?) {
        logD(String.format("connectedToEndpoint(endpoint=%s)", endpoint))
        establishedConnections.put(endpoint!!.id, endpoint)
        onEndpointConnected(endpoint)
    }

    private fun disconnectedFromEndpoint(endpoint: Endpoint?) {
        logD(String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint))
        establishedConnections.remove(endpoint?.id)
        onEndpointDisconnected(endpoint)
    }

    /** A connection with this endpoint has failed. Override this method to act on the event.  */
    protected fun onConnectionFailed(endpoint: Endpoint?) {}

    /** Someone has connected to us. Override this method to act on the event.  */
    protected fun onEndpointConnected(endpoint: Endpoint?) {}

    /** Someone has disconnected. Override this method to act on the event.  */
    protected fun onEndpointDisconnected(endpoint: Endpoint?) {}

    /** @return A list of currently connected endpoints.
     */
    protected fun getDiscoveredEndpoints(): MutableSet<Endpoint> {
        val endpoints = HashSet<Endpoint>()
        endpoints += discoveredEndpoints.values
        return endpoints
    }

    /** @return A list of currently connected endpoints. */
    protected fun getConnectedEndpoints(): MutableSet<Endpoint> {
        val endpoints = HashSet<Endpoint>()
        endpoints.addAll(establishedConnections.values)
        return endpoints
    }

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     *
     * @param payload The data you want to send.
     */
    protected fun send(payload: Payload) {
        send(payload, establishedConnections.keys);
    }

    private fun send(payload: Payload, endpoints: MutableSet<String>) {
        Nearby.Connections.sendPayload(googleApiClient, ArrayList(endpoints), payload)
                .setResultCallback(
                        ResultCallback<Status>() {
                            fun onResult(status: Status) {
                                if (!status.isSuccess()) {
                                    logW(
                                            String.format(
                                                    "sendUnreliablePayload failed. %s",
                                                    AttendanceActivity.toString(status)));
                                }
                            }
                        })

    }




    /**
     * Someone connected to us has sent us data. Override this method to act on the event.
     *
     * @param endpoint The sender.
     * @param payload The data.
     */
    protected fun onReceive(endpoint: Endpoint?, payload: Payload) {}

    /**
     * An optional hook to pool any permissions the app needs with the permissions ConnectionsActivity
     * will request.
     *
     * @return All permissions required for the app to properly function.
     */
    protected fun getRequiredPermissions(): Array<String> {
        return REQUIRED_PERMISSIONS
    }

    /** @return The client's name. Visible to others when connecting.
     */
    protected abstract fun getName(): String

    /**
     * @return The service id. This represents the action this connection is for. When discovering,
     * we'll verify that the advertiser has the same service id before we consider connecting to
     * them.
     */
    protected abstract fun getServiceId(): String

    /**
     * Transforms a [Status] into a English-readable message for logging.
     *
     * @param status The current status
     * @return A readable String. eg. [404]File not found.
     */
    companion object {
        private fun toString(status: Status): String {
            return String.format(
                    Locale.US,
                    "[%d]%s",
                    status.statusCode,
                    if (status.statusMessage != null)
                        status.statusMessage
                    else
                        ConnectionsStatusCodes.getStatusCodeString(status.statusCode))
        }
    }


    /** @return True if the app was granted all the permissions. False otherwise.
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun logV(msg: String) {
        Log.v(TAG, msg)
    }

    fun logD(msg: String) {
        Log.d(TAG, msg)
    }

    fun logW(msg: String) {
        Log.w(TAG, msg)
    }

    fun logE(msg: String) {
        Log.e(TAG, msg)
    }

}

