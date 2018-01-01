package com.example.shubham.attendancesystemteacherversion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.common.api.GoogleApiClient
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import org.jetbrains.anko.longToast
import com.google.android.gms.nearby.connection.ConnectionInfo
import android.content.Context
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import java.util.*
import android.support.v4.content.ContextCompat




abstract class ConnectionsActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    /**
     * These permissions are required before connecting to Nearby Connections. Only [ ][Manifest.permission.ACCESS_COARSE_LOCATION] is considered dangerous, so the others should be
     * granted just by having them in our AndroidManfiest.xml
     */
    private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

    private val TAG = "TEACHER"

    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
     */
    private val STRATEGY = Strategy.P2P_STAR

    /** We'll talk to Nearby Connections through the GoogleApiClient.  */
    private var mGoogleApiClient: GoogleApiClient? = null

    /** The devices we've discovered near us.  */
    private val mDiscoveredEndpoints = HashMap<String, Endpoint>()

    /**
     * The devices we have pending connections to. They will stay pending until we call [ ][.acceptConnection] or [.rejectConnection].
     */
    private val mPendingConnections = HashMap<String, Endpoint>()

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     */
    private val mEstablishedConnections = HashMap<String, Endpoint>()

    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * device.
     */
    private var mIsConnecting = false

    /** True if we are discovering.  */
    private var mIsDiscovering = false

    /** True if we are advertising.  */
    private var mIsAdvertising = false

    /** Callbacks for connections to other devices. */
    private val mConnectionLifecycleCallback = object: ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "onConnectionInitiated(endpointId=$endpointId, endpointName=${connectionInfo.endpointName}")
            val endpoint = Endpoint(endpointId, connectionInfo.endpointName)
            mPendingConnections.put(endpointId, endpoint)
            this@ConnectionsActivity.onConnectionInitiated(endpoint, connectionInfo)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "onConnectionResponse(endpointId=$endpointId, result=$result")

            // We're no longer connecting
            mIsConnecting = false

            if (!result.status.isSuccess) {
                Log.w(TAG, "Connection failed. Received status ${ConnectionsActivity.toString(result.status)}")
                onConnectionFailed(mPendingConnections.remove(endpointId)!!)
                return
            }

            connectedToEndpoint(mPendingConnections.remove(endpointId)!!)
        }

        override fun onDisconnected(endpointId: String) {
            if (!mEstablishedConnections.containsKey(endpointId)) {
                Log.w(TAG, "Unexpected disconnection from endpoint $endpointId")
                return
            }
            disconnectedFromEndpoint(mEstablishedConnections[endpointId]!!)
        }
    }

    /** Callbacks for payloads (bytes of data) sent from another device to us. */
    private val mPayloadCallback = object: PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(TAG, "onPayloadReceived(endpointId=$endpointId, payload=$payload")
            onReceive(mEstablishedConnections[endpointId]!!, payload)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(TAG, "onPayloadTransferUpdate(endpointId=$endpointId, udpate=$update")
        }
    }

    private fun resetState() {
        mDiscoveredEndpoints.clear()
        mPendingConnections.clear()
        mEstablishedConnections.clear()
        mIsConnecting = false
        mIsAdvertising = false
        mIsDiscovering = false
    }

    private fun createGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(this)
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
        if (hasPermissions(this, REQUIRED_PERMISSIONS)) {
            createGoogleApiClient()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
        }
        super.onStart()
    }

    /** We've connected to Nearby Connections' GoogleApiClient. */
    override fun onConnected(bundle: Bundle?) {
        Log.v(TAG, "onConnected")
    }

    /** We've been temporarily disconnected from Nearby Connections' GoogleApiClient. */
    @CallSuper
    override fun onConnectionSuspended(reason: Int) {
        Log.w(TAG, "onConnectionSuspended(reason=$reason")
        resetState()
    }

    /** We are unable to connect to Nearby Connections' GoogleApiClient. Oh uh. */
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.w(TAG, "onConnectionFailed(${ConnectionsActivity.toString(Status(connectionResult.errorCode))})")
    }

    /** The user has accepted (or denied) our permission request. */
    @CallSuper
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            for (grantResult in grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    longToast(R.string.error_missing_permissions)
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
     * Either {@link #onAdvertisingStarted()} or {@link #onAdvertisingFailed()} will be called once
     * we've found out if we successfully entered this mode.
     */
    protected fun startAdvertising() {
        mIsAdvertising = true

        Nearby.Connections.startAdvertising(
                mGoogleApiClient,
                getName(),
                getServiceId(),
                mConnectionLifecycleCallback,
                AdvertisingOptions(STRATEGY))
                .setResultCallback {
                    object: ResultCallback<Connections.StartAdvertisingResult> {
                        override fun onResult(result: Connections.StartAdvertisingResult) {
                            if (result.status.isSuccess) {
                                Log.v(TAG, "Now advertising endpoint ${result.localEndpointName}")
                                onAdvertisingStarted()
                            } else {
                                mIsAdvertising = false
                                Log.w(TAG, "Advertising Failed. Received Status ${ConnectionsActivity.toString(result.status)}")
                                onAdvertisingFailed()
                            }
                        }
                    }
                }
    }

    /** Stops advertising.  */
    protected fun stopAdvertising() {
        mIsAdvertising = false
        Nearby.Connections.stopAdvertising(mGoogleApiClient)
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

    /** Accepts a connection request. */
    protected fun acceptConnection(endpoint: Endpoint) {
        Nearby.Connections.acceptConnection(mGoogleApiClient, endpoint.id, mPayloadCallback)
                .setResultCallback {
                    object: ResultCallback<Status> {
                        override fun onResult(status: Status) {
                            if (!status.isSuccess) {
                                Log.w(TAG, "acceptConnection Failed. ${ConnectionsActivity.toString(status)}")
                            }
                        }
                    }
                }
    }

    /** Rejects a connection request. */
    protected fun rejectConnection(endpoint: Endpoint) {
        Nearby.Connections.rejectConnection(mGoogleApiClient, endpoint.id)
                .setResultCallback {
                    object: ResultCallback<Status> {
                        override fun onResult(status: Status) {
                            if (!status.isSuccess) {
                                Log.w(TAG, "rejectConnection failed. ${ConnectionsActivity.toString(status)}")
                            }
                        }
                    }
                }
    }

    /**
     * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
     * {@link #onDiscoveryStarted()} ()} or {@link #onDiscoveryFailed()} ()} will be called once we've
     * found out if we successfully entered this mode.
     */
    protected fun startDiscovering() {
        mIsDiscovering = true
        mDiscoveredEndpoints.clear()
        Nearby.Connections.startDiscovery(
                mGoogleApiClient,
                getServiceId(),
                object: EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        Log.d(TAG, "onEndpointFound(endpointId=$endpointId, serviceId=${info.serviceId}, endpointName=${info.endpointName}")

                        if (getServiceId().equals(info.serviceId)) {
                            val endpoint = Endpoint(endpointId, info.endpointName)
                            mDiscoveredEndpoints.put(endpointId, endpoint)
                            onEndpointDiscovered(endpoint)
                        }
                    }

                    override fun onEndpointLost(endpointId: String) {
                        Log.d(TAG, "onEndpointLost(endpointId=$endpointId")
                    }
                },
                DiscoveryOptions(STRATEGY))
                .setResultCallback {
                    object: ResultCallback<Status> {
                        override fun onResult(status: Status) {
                            if (status.isSuccess) {
                                onDiscoveryStarted()
                            } else {
                                mIsDiscovering = false
                                Log.w(TAG, "Discovering failed. Received status ${ConnectionsActivity.toString(status)}")
                                onDiscoveryFailed()
                            }
                        }
                    }
                }
    }

    /** Stops discovery.  */
    protected fun stopDiscovering() {
        mIsDiscovering = false
        Nearby.Connections.stopDiscovery(mGoogleApiClient)
    }

    /** @return True if currently discovering.
     */
    protected fun isDiscovering(): Boolean {
        return mIsDiscovering
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
        Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpoint.id)
        mEstablishedConnections.remove(endpoint.id)
    }

    protected fun disconnectFromAllEndpoints() {
        for (endpoint in mEstablishedConnections.values) {
            Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpoint.id)
        }
        mEstablishedConnections.clear()
    }

    /** Sends a connection request to the endpoint. */
    protected fun connectToEndpoint(endpoint: Endpoint) {
        // If we already sent out a connection request, wait for it to return
        // before we do anything else. P2P_STAR only allows 1 outgoing connection.

        if (mIsConnecting) {
            Log.w(TAG, "Already connecting, so ignoring this endpoint: $endpoint")
            return
        }
        Log.v(TAG, "Sending a connection request to endpoint $endpoint")

        // Mark ourselves as connecting so we don't connect multiple times
        mIsConnecting = true

        // Ask to connect
        Nearby.Connections.requestConnection(
                mGoogleApiClient,
                getName(),
                endpoint.id,
                mConnectionLifecycleCallback)
                .setResultCallback {
                    object: ResultCallback<Status> {
                        override fun onResult(status: Status) {
                            if (!status.isSuccess) {
                                Log.w(TAG, "requestConnection failed. ${ConnectionsActivity.toString(status)}")
                                mIsConnecting = false
                                onConnectionFailed(endpoint)
                            }
                        }
                    }
                }
    }

    /** True if we're currently attempting to connect to another device.  */
    protected fun isConnecting(): Boolean {
        return mIsConnecting
    }

    private fun connectedToEndpoint(endpoint: Endpoint) {
        Log.d(TAG, "connectedToEndpoint(endpoint=$endpoint)")
        mEstablishedConnections.put(endpoint.id, endpoint)
        onEndpointConnected(endpoint)
    }

    private fun disconnectedFromEndpoint(endpoint: Endpoint) {
        Log.d(TAG, "disconnectedFromEndpoint(endpoint=$endpoint)")
        mEstablishedConnections.remove(endpoint.id)
        onEndpointDisconnected(endpoint)
    }

    /** A connection with this endpoint has failed. Override this method to act on the event.  */
    protected fun onConnectionFailed(endpoint: Endpoint) {}

    /** Someone has connected to us. Override this method to act on the event.  */
    protected fun onEndpointConnected(endpoint: Endpoint) {}

    /** Someone has disconnected. Override this method to act on the event.  */
    protected fun onEndpointDisconnected(endpoint: Endpoint) {}

    /** @return A list of currently connected endpoints.
     */
    protected fun getDiscoveredEndpoints(): Set<Endpoint> {
        val endpoints = HashSet<Endpoint>()
        endpoints.addAll(mDiscoveredEndpoints.values)
        return endpoints
    }

    /** @return A list of currently connected endpoints.
     */
    protected fun getConnectedEndpoints(): Set<Endpoint> {
        val endpoints = HashSet<Endpoint>()
        endpoints.addAll(mEstablishedConnections.values)
        return endpoints
    }

    /**
     * Sends a [Payload] to all currently connected endpoints.
     *
     * @param payload The data you want to send.
     */
    protected fun send(payload: Payload) {
        send(payload, mEstablishedConnections.keys)
    }

    private fun send(payload: Payload, endpoints: Set<String>) {
        Nearby.Connections.sendPayload(mGoogleApiClient, ArrayList(endpoints), payload)
                .setResultCallback {
                    object: ResultCallback<Status> {
                        override fun onResult(status: Status) {
                            if (!status.isSuccess) {
                                Log.w(TAG, "sendUnreliablePayload failed. ${ConnectionsActivity.toString(status)}")
                            }
                        }
                    }
                }
    }

    /**
     * Someone connected to us has sent us data. Override this method to act on the event.
     *
     * @param endpoint The sender.
     * @param payload The data.
     */
    protected fun onReceive(endpoint: Endpoint, payload: Payload) {}

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

    companion object {
        /**
         * Transforms a [Status] into a English-readable message for logging.
         *
         * @param status The current status
         * @return A readable String. eg. [404]File not found.
         */
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
    private fun hasPermissions(context: Context, permissions: Array<out String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}
