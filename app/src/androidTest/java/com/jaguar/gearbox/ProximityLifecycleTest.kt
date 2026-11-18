package com.jaguar.gearbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.components.rememberProximityState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class ProximityLifecycleTest {
    @get:Rule val rule=createComposeRule()
    @Test fun registersOnlyStartedAndDisposalUnregisters() {
        val owner=object:LifecycleOwner {
            val registry=LifecycleRegistry(this)
            override val lifecycle:Lifecycle get()=registry
        }
        var starts=0;var stops=0
        var reading:((Float,Int)->Unit)?=null
        val source=object:ProximitySource {
            override val available=true
            override val maximumRange=1000f
            override val resolution=1f
            override fun start(onReading:(Float,Int)->Unit,onAccuracy:(Int)->Unit):Boolean { starts++;reading=onReading;return true }
            override fun stop(){stops++}
        }
        var visible by mutableStateOf(true)
        rule.runOnUiThread { owner.registry.currentState=Lifecycle.State.CREATED }
        rule.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
            if(visible) { val state=rememberProximityState(source); Text("${state.status}:${state.distanceCm}") }
        } }
        rule.runOnIdle { assertEquals(0,starts);owner.registry.currentState=Lifecycle.State.STARTED }
        rule.onNodeWithText("WAITING:null").assertExists()
        rule.runOnIdle { reading!!(42f,3) }
        rule.onNodeWithText("READING:42.0").assertExists()
        rule.runOnIdle { owner.registry.currentState=Lifecycle.State.CREATED;reading!!(50f,3) }
        rule.onNodeWithText("STOPPED:null").assertExists()
        rule.runOnIdle { assertEquals(1,stops);owner.registry.currentState=Lifecycle.State.STARTED }
        rule.onNodeWithText("WAITING:null").assertExists()
        rule.runOnIdle { assertEquals(2,starts);visible=false }
        rule.runOnIdle { assertEquals(2,stops) }
    }
}
