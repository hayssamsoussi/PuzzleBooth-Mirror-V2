package com.puzzlebooth.main

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Build
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import androidx.core.view.ViewCompat
import androidx.navigation.findNavController
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import com.puzzlebooth.main.base.MessageEvent
import com.puzzlebooth.main.utils.ConnectionsActivity
import com.puzzlebooth.server.R
import org.greenrobot.eventbus.EventBus
import java.util.Random

open class BaseNearbyActivity: ConnectionsActivity() {

    var count = 0

    enum class State {
        UNKNOWN, SEARCHING, CONNECTED
    }

    /** If true, debug logs are shown on the device.  */
    public val DEBUG = true

    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
     */
    private val STRATEGY = Strategy.P2P_STAR

    /** Length of state change animations.  */
    private val ANIMATION_DURATION: Long = 600

    /**
     * A set of background colors. We'll hash the authentication token we get from connecting to a
     * device to pick a color randomly from this list. Devices with the same background color are
     * talking to each other securely (with 1/COLORS.length chance of collision with another pair of
     * devices).
     */
    @ColorInt
    private val COLORS = intArrayOf(
        -0xbbcca /* red */,
        -0x63d850 /* deep purple */,
        -0xff432c /* teal */,
        -0xb350b0 /* green */,
        -0x5500 /* amber */,
        -0x6800 /* orange */,
        -0x86aab8 /* brown */
    )

    /**
     * This service id lets us find other nearby devices that are interested in the same thing. Our
     * sample does exactly one thing, so we hardcode the ID.
     */
    private val SERVICE_ID = "com.google.location.nearby.apps.walkietalkie.automatic.SERVICE_ID"

    /**
     * The state of the app. As the app changes states, the UI will update and advertising/discovery
     * will start/stop.
     */
    private var mState: State = State.UNKNOWN

    /** A random UID used as this device's endpoint name.  */
    var mName: String? = null

    /**
     * The background color of the 'CONNECTED' state. This is randomly chosen from the [.COLORS]
     * list, based off the authentication token.
     */
    @ColorInt
    private var mConnectedColor = COLORS[0]

    /** Displays the previous state during animation transitions.  */
    var mPreviousStateView: TextView? = null

    /** Displays the current state.  */
    var mCurrentStateView: TextView? = null

    /** An animator that controls the animation from previous state to current state.  */
    private var mCurrentAnimator: Animator? = null

    /** A running log of debug messages. Only visible when DEBUG=true.  */
    var mDebugLogView: TextView? = null

    override fun onStart() {
        super.onStart()
        setState(State.SEARCHING)
    }

    override fun onStop() {
        // After our Activity stops, we disconnect from Nearby Connections.
        setState(State.UNKNOWN)
        if (mCurrentAnimator != null && mCurrentAnimator!!.isRunning) {
            mCurrentAnimator!!.cancel()
        }
        super.onStop()
    }
    override fun onEndpointDiscovered(endpoint: Endpoint?) {
        // We found an advertiser!
        stopDiscovering()
        connectToEndpoint(endpoint)
    }

    override fun onConnectionInitiated(endpoint: Endpoint?, connectionInfo: ConnectionInfo?) {
        // A connection to another device has been initiated! We'll use the auth token, which is the
        // same on both devices, to pick a color to use when we're connected. This way, users can
        // visually see which device they connected with.
        mConnectedColor = COLORS[connectionInfo?.authenticationToken.hashCode() % COLORS.size]

        // We accept the connection immediately.
        acceptConnection(endpoint)
    }

    override fun onEndpointConnected(endpoint: Endpoint?) {
        Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show()
        setState(State.CONNECTED)
    }

    override fun onEndpointDisconnected(endpoint: Endpoint?) {
        Toast.makeText(this, "Disconnected!", Toast.LENGTH_SHORT).show()
        setState(State.SEARCHING)
    }

    override fun onConnectionFailed(endpoint: Endpoint?) {
        // Let's try someone else.
        if (getState() == State.SEARCHING) {
            startDiscovering()
        }
    }

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */
    private fun setState(state: State) {
        println("hhh state ${state.name}")
        toggleRemoteDot(state)
        EventBus.getDefault().post(MessageEvent(state.name))
        if (mState == state) {
            logW("State set to $state but already in that state")
            return
        }
        logD("State set to $state")
        val oldState: State = mState
        mState = state
        onStateChanged(oldState, state)
    }

    /** @return The current state.
     */
    private fun getState(): State {
        return mState
    }

    /**
     * State has changed.
     *
     * @param oldState The previous state we were in. Clean up anything related to this state.
     * @param newState The new state we're now in. Prepare the UI for this state.
     */
    private fun onStateChanged(
        oldState: State,
        newState: State
    ) {
        if (mCurrentAnimator != null && mCurrentAnimator!!.isRunning()) {
            mCurrentAnimator!!.cancel()
        }
        when (newState) {
            State.SEARCHING -> {
                disconnectFromAllEndpoints()
                startDiscovering()
                startAdvertising()
            }

            State.CONNECTED -> {
                stopDiscovering()
                stopAdvertising()
            }

            State.UNKNOWN -> stopAllEndpoints()
            else -> {}
        }
        when (oldState) {
            State.UNKNOWN ->         // Unknown is our initial state. Whatever state we move to,
                // we're transitioning forwards.
                transitionForward(oldState, newState)

            State.SEARCHING -> when (newState) {
                State.UNKNOWN -> transitionBackward(oldState, newState)
                State.CONNECTED -> transitionForward(oldState, newState)
                else -> {}
            }

            State.CONNECTED ->         // Connected is our final state. Whatever new state we move to,
                // we're transitioning backwards.
                transitionBackward(oldState, newState)
        }
    }

    /** Transitions from the old state to the new state with an animation implying moving forward.  */
    @UiThread
    private fun transitionForward(
        oldState: State,
        newState: State
    ) {
        mPreviousStateView?.visibility = View.VISIBLE
        mCurrentStateView?.visibility = View.VISIBLE

        mPreviousStateView?.let { updateTextView(it, oldState) }
        mCurrentStateView?.let { updateTextView(it, newState) }
        if (mCurrentStateView?.let { ViewCompat.isLaidOut(it) } == true) {
            mCurrentAnimator = createAnimator(false /* reverse */)
            mCurrentAnimator?.addListener(
                object : AnimatorListener() {
                    override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                        updateTextView(mCurrentStateView!!, newState)
                    }
                })
            mCurrentAnimator!!.start()
        }
    }

    /** Transitions from the old state to the new state with an animation implying moving backward.  */
    @UiThread
    private fun transitionBackward(
        oldState: State,
        newState: State
    ) {
        mPreviousStateView?.visibility = View.VISIBLE
        mCurrentStateView?.visibility = View.VISIBLE
        mCurrentStateView?.let { updateTextView(it, oldState) }
        mPreviousStateView?.let { updateTextView(it, newState) }
        if (mCurrentStateView?.let { ViewCompat.isLaidOut(it) } == true) {
            mCurrentAnimator = createAnimator(true /* reverse */)
            mCurrentAnimator?.addListener(
                object : AnimatorListener() {
                    override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                        mCurrentStateView?.let {
                            updateTextView(it, newState)
                        }

                    }
                })
            mCurrentAnimator?.start()
        }
    }

    private fun createAnimator(reverse: Boolean): Animator? {
        val animator: Animator
        if (Build.VERSION.SDK_INT >= 21) {
            val cx = mCurrentStateView?.measuredWidth?.div(2) ?: return null
            val cy = mCurrentStateView?.measuredHeight?.div(2) ?: return null
            var initialRadius = 0
            var finalRadius = Math.max(mCurrentStateView!!.width, mCurrentStateView!!.height)
            if (reverse) {
                val temp = initialRadius
                initialRadius = finalRadius
                finalRadius = temp
            }

            animator = ViewAnimationUtils.createCircularReveal(
                mCurrentStateView, cx, cy, initialRadius.toFloat(), finalRadius.toFloat()
            )
        } else {
            var initialAlpha = 0f
            var finalAlpha = 1f
            if (reverse) {
                val temp = initialAlpha
                initialAlpha = finalAlpha
                finalAlpha = temp
            }
            mCurrentStateView!!.alpha = initialAlpha
            animator = ObjectAnimator.ofFloat(mCurrentStateView, "alpha", finalAlpha)
        }
        animator.addListener(
            object : AnimatorListener() {
                override fun onAnimationCancel(animator: Animator) {
                    mPreviousStateView!!.visibility = View.GONE
                    mCurrentStateView!!.alpha = 1f
                }

                override fun onAnimationEnd(animator: Animator) {
                    mPreviousStateView!!.visibility = View.GONE
                    mCurrentStateView!!.alpha = 1f
                }
            })
        animator.duration = ANIMATION_DURATION
        return animator
    }

    /** Updates the [TextView] with the correct color/text for the given [State].  */
    @UiThread
    private fun updateTextView(
        textView: TextView,
        state: State
    ) {
        when (state) {
            State.SEARCHING -> {
                //textView.setBackgroundResource(R.color.state_searching)
                textView.setText("Searching")
            }

            State.CONNECTED -> {
                textView.setBackgroundColor(mConnectedColor)
                textView.setText("Connected")
            }

            else -> {
                //textView.setBackgroundResource(R.color.state_unknown)
                textView.setText("Unknown")
            }
        }
    }

    /** {@see ConnectionsActivity#onReceive(Endpoint, Payload)}  */
    override fun onReceive(endpoint: Endpoint?, payload: Payload?) {
        val event = payload?.asBytes()?.let { String(it) }
        if(!event.isNullOrEmpty())
            EventBus.getDefault().post(MessageEvent(event))
    }

    /** {@see ConnectionsActivity#getRequiredPermissions()}  */
    override fun getRequiredPermissions(): Array<String?>? {
        return join(
            super.getRequiredPermissions(),
            Manifest.permission.RECORD_AUDIO
        )
    }

    /** Joins 2 arrays together.  */
    private fun join(a: Array<String>, vararg b: String): Array<String?>? {
        val join = arrayOfNulls<String>(a.size + b.size)
        System.arraycopy(a, 0, join, 0, a.size)
        System.arraycopy(b, 0, join, a.size, b.size)
        return join
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    override fun getName(): String? {
        return mName
    }

    /** {@see ConnectionsActivity#getServiceId()}  */
    override fun getServiceId(): String {
        return SERVICE_ID
    }

    /** {@see ConnectionsActivity#getStrategy()}  */
    override fun getStrategy(): Strategy {
        return STRATEGY
    }

    override fun logV(msg: String) {
        super.logV(msg)
        appendToLogs(toColor(msg, Color.RED))
    }

    override fun logD(msg: String) {
        super.logD(msg)
        appendToLogs(toColor(msg, Color.BLUE))
    }

    override fun logW(msg: String) {
        super.logW(msg)
        appendToLogs(toColor(msg, Color.YELLOW))
    }

    override fun logW(msg: String, e: Throwable?) {
        super.logW(msg, e)
        appendToLogs(toColor(msg, Color.MAGENTA))
    }

    override fun logE(msg: String, e: Throwable?) {
        super.logE(msg, e)
        appendToLogs(toColor(msg, Color.RED))
    }

    private fun appendToLogs(msg: CharSequence) {
        mDebugLogView?.append("\n")
        mDebugLogView?.append(
            DateFormat.format("hh:mm", System.currentTimeMillis()).toString() + ": "
        )
        mDebugLogView?.append(msg)
    }

    private fun toColor(msg: String, color: Int): CharSequence {
        val spannable = SpannableString(msg)
        spannable.setSpan(ForegroundColorSpan(color), 0, msg.length, 0)
        return spannable
    }

    fun generateRandomName(): String? {
        var name = ""
        val random = Random()
        for (i in 0..4) {
            name += random.nextInt(10)
        }
        return name
    }

    /**
     * Provides an implementation of Animator.AnimatorListener so that we only have to override the
     * method(s) we're interested in.
     */
    private abstract class AnimatorListener : Animator.AnimatorListener {
        override fun onAnimationStart(animator: Animator) {}
        override fun onAnimationEnd(animator: Animator) {}
        override fun onAnimationCancel(animator: Animator) {}
        override fun onAnimationRepeat(animator: Animator) {}
    }

    override fun onBackPressed() {

//        if (getState() == State.CONNECTED) {
//            setState(State.SEARCHING)
//            findNavController(R.id.nav_host_fragment).setGraph(
//                R.navigation.nav_graph
//            )
//            return
//        }
        findNavController(R.id.nav_host_fragment).setGraph(R.navigation.nav_graph)
        return
        super.onBackPressed()
        //super.onBackPressed()
    }



    open fun togglePrinterDot(isOnline: Boolean) {

    }

    open fun toggleRemoteDot(state: State) {

    }
}