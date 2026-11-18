package com.jaguar.gearbox.ui.components

import android.content.*
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.sensors.*

private class AndroidBatteryInfoSource(private val context: Context) : BatteryInfoSource {
    private var receiver: BroadcastReceiver? = null
    private val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private fun snapshot(intent: Intent): BatterySnapshot {
        fun extra(key: String): Int? = if (intent.hasExtra(key)) intent.getIntExtra(key, Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE } else null
        fun property(id: Int): Int? = runCatching { manager?.getIntProperty(id) }.getOrNull()
        return sanitizeBatteryPresence(BatterySnapshot(
            present = if (intent.hasExtra(BatteryManager.EXTRA_PRESENT)) intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false) else null,
            percent = batteryPercent(extra(BatteryManager.EXTRA_LEVEL), extra(BatteryManager.EXTRA_SCALE)),
            temperatureCelsius = extra(BatteryManager.EXTRA_TEMPERATURE)?.takeIf { it in -1000..2000 }?.div(10.0),
            voltageVolts = extra(BatteryManager.EXTRA_VOLTAGE)?.takeIf { it in 1..100000 }?.div(1000.0),
            currentMilliamps = batteryCurrentMilliamps(property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)),
            chargeMilliampHours = batteryChargeMilliampHours(property(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)),
            status = when (extra(BatteryManager.EXTRA_STATUS)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
                BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
                BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
                else -> BatteryStatus.UNKNOWN
            },
            power = when (extra(BatteryManager.EXTRA_PLUGGED)) {
                0 -> BatteryPower.BATTERY
                BatteryManager.BATTERY_PLUGGED_AC -> BatteryPower.AC
                BatteryManager.BATTERY_PLUGGED_USB -> BatteryPower.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryPower.WIRELESS
                8 -> BatteryPower.DOCK
                else -> BatteryPower.UNKNOWN
            },
            health = when (extra(BatteryManager.EXTRA_HEALTH)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
                BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.FAILURE
                BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
                else -> BatteryHealth.UNKNOWN
            },
        ))
    }
    override fun start(onSnapshot: (BatterySnapshot) -> Unit): Boolean {
        val callback = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) onSnapshot(snapshot(intent))
            }
        }
        receiver = callback
        val sticky = ContextCompat.registerReceiver(context, callback, IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            null, Handler(Looper.getMainLooper()), ContextCompat.RECEIVER_NOT_EXPORTED)
        if (sticky != null) onSnapshot(snapshot(sticky))
        return true
    }
    override fun stop() {
        val old = receiver
        receiver = null
        if (old != null) context.unregisterReceiver(old)
    }
}
@Composable
fun rememberBatteryInfoState(source: BatteryInfoSource? = null): BatteryInfoState {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val actual = remember(context, source) { source ?: AndroidBatteryInfoSource(context.applicationContext) }
    var state by remember(actual) { mutableStateOf(BatteryInfoState()) }
    DisposableEffect(actual, lifecycle) {
        val controller = BatteryInfoController(actual) { state = it }
        val observer = LifecycleEventObserver { _, _ ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controller.start() else controller.stop()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controller.start()
        onDispose { lifecycle.removeObserver(observer); controller.stop() }
    }
    return state
}
