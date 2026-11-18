package com.jaguar.gearbox
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test
class BatteryInfoTest {
    @Test fun percentUnitsAndUnsupportedSentinels() {
        assertEquals(50,batteryPercent(1,2))
        assertEquals(67,batteryPercent(2,3))
        assertEquals(100,batteryPercent(Int.MAX_VALUE,Int.MAX_VALUE))
        assertNull(batteryPercent(1,0)); assertNull(batteryPercent(-1,100));assertNull(batteryPercent(101,100))
        assertNull(batteryPercent(null,100))
        assertEquals(-123.456,batteryCurrentMilliamps(-123456)!!,1e-9)
        assertEquals(0.0,batteryCurrentMilliamps(0)!!,0.0)
        assertNull(batteryCurrentMilliamps(Int.MIN_VALUE));assertNull(batteryCurrentMilliamps(Int.MAX_VALUE))
        assertEquals(1500.0,batteryChargeMilliampHours(1500000)!!,0.0)
        assertNull(batteryChargeMilliampHours(-1));assertNull(batteryChargeMilliampHours(Int.MAX_VALUE))
    }
    @Test fun noBatterySuppressesAllReadingsButMissingPresenceIsUnknown() {
        val reported=BatterySnapshot(present=false,percent=0,voltageVolts=4.0,currentMilliamps=10.0,
            status=BatteryStatus.CHARGING,health=BatteryHealth.GOOD)
        assertEquals(BatterySnapshot(present=false),sanitizeBatteryPresence(reported))
        assertEquals(reported.copy(present=null),sanitizeBatteryPresence(reported.copy(present=null)))
    }
    @Test fun staleCallbacksAndRegistrationFailureCannotReviveReadings() {
        var callback:((BatterySnapshot)->Unit)?=null
        var stops=0
        var shouldFail=false
        val source=object:BatteryInfoSource {
            override fun start(onSnapshot:(BatterySnapshot)->Unit):Boolean {
                callback=onSnapshot
                if(shouldFail) { onSnapshot(BatterySnapshot(percent=90));throw SecurityException() }
                return true
            }
            override fun stop(){stops++}
        }
        var state=BatteryInfoState()
        val controller=BatteryInfoController(source){state=it}
        controller.start();val old=callback!!
        old(BatterySnapshot(percent=50));assertEquals(50,state.snapshot!!.percent)
        controller.stop();old(BatterySnapshot(percent=60));assertEquals(BatteryInfoState(),state)
        controller.start();old(BatterySnapshot(percent=60));assertEquals(BatteryInfoStatus.WAITING,state.status)
        controller.stop();shouldFail=true;controller.start()
        callback!!(BatterySnapshot(percent=80));assertEquals(BatteryInfoStatus.UNAVAILABLE,state.status)
        assertTrue(stops>=3)
    }
    @Test fun noRetention() {
        val route="tool/battery_information"
        assertFalse(ToolMemoryPolicy.permitsRoute(route));assertFalse(ToolMemoryPolicy.permitsModeRoute(route))
        assertFalse(canRestoreToolSnapshot(route))
        val settings=ToolMemorySettings(historyEnabled=true,rememberResults=true,automaticMemory=mapOf(route to ToolAutomaticMemory(true,true)))
        assertFalse(settings.permitsHistory(route));assertFalse(settings.permitsResult(route))
    }
}
