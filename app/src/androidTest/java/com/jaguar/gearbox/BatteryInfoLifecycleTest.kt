package com.jaguar.gearbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.components.rememberBatteryInfoState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class BatteryInfoLifecycleTest {
    @get:Rule val rule=createComposeRule()
    @Test fun receiverFollowsForegroundAndStaleEventsStayCleared() {
        val owner=object:LifecycleOwner {
            val registry=LifecycleRegistry(this)
            override val lifecycle:Lifecycle get()=registry
        }
        var starts=0;var stops=0
        var callback:((BatterySnapshot)->Unit)?=null
        val source=object:BatteryInfoSource {
            override fun start(onSnapshot:(BatterySnapshot)->Unit):Boolean {starts++;callback=onSnapshot;return true}
            override fun stop(){stops++}
        }
        var visible by mutableStateOf(true)
        rule.runOnUiThread {owner.registry.currentState=Lifecycle.State.CREATED}
        rule.setContent {CompositionLocalProvider(LocalLifecycleOwner provides owner) {
            if(visible) {val value=rememberBatteryInfoState(source);Text("${value.status}:${value.snapshot?.percent}")}
        }}
        rule.runOnIdle {assertEquals(0,starts);owner.registry.currentState=Lifecycle.State.STARTED}
        rule.onNodeWithText("WAITING:null").assertExists()
        rule.runOnIdle {callback!!(BatterySnapshot(percent=42))}
        rule.onNodeWithText("AVAILABLE:42").assertExists()
        val old=callback!!
        rule.runOnIdle {owner.registry.currentState=Lifecycle.State.CREATED;old(BatterySnapshot(percent=99))}
        rule.onNodeWithText("STOPPED:null").assertExists()
        rule.runOnIdle {owner.registry.currentState=Lifecycle.State.STARTED;old(BatterySnapshot(percent=99))}
        rule.onNodeWithText("WAITING:null").assertExists()
        rule.runOnIdle {assertEquals(2,starts);visible=false}
        rule.runOnIdle {assertTrue(stops>=2)}
    }
}
