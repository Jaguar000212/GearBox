package com.jaguar.gearbox.ui.components
import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraState
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.camera.*

class MagnifierHandle(val state:MagnifierState,val previewView:PreviewView,val start:()->Unit,val stop:()->Unit,val freeze:()->Unit,val resume:()->Unit,val zoom:(Float)->Unit,val torch:()->Unit)
@Composable fun rememberMagnifier():MagnifierHandle {
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    val view=remember(context){PreviewView(context).apply{implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FIT_CENTER}}
    var state by remember{mutableStateOf(MagnifierState())}
    var alive by remember{mutableStateOf(true)}
    var epoch by remember{mutableLongStateOf(0)}
    var zoomRequest by remember{mutableLongStateOf(0)}
    var torchRequest by remember{mutableLongStateOf(0)}
    var provider by remember{mutableStateOf<ProcessCameraProvider?>(null)}
    var preview by remember{mutableStateOf<Preview?>(null)}
    var camera by remember{mutableStateOf<Camera?>(null)}
    var cameraObserver by remember { mutableStateOf<Observer<CameraState>?>(null) }
    fun cleanup(){
        ++epoch; ++zoomRequest; ++torchRequest
        val oldCamera=camera;val oldPreview=preview;val oldProvider=provider
        val oldObserver=cameraObserver
        camera=null;preview=null;provider=null;cameraObserver=null
        runCatching{if(oldObserver!=null)oldCamera?.cameraInfo?.cameraState?.removeObserver(oldObserver)}
        runCatching{oldCamera?.cameraControl?.enableTorch(false)}
        runCatching{if(oldPreview!=null)oldProvider?.unbind(oldPreview)}
        state=MagnifierState()
    }
    fun start(){
        if(!alive || !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))return
        cleanup()
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){state=MagnifierState(MagnifierStatus.PERMISSION_REQUIRED);return}
        val token=epoch;state=MagnifierState(MagnifierStatus.STARTING)
        val future=ProcessCameraProvider.getInstance(context)
        future.addListener({
            if(alive && token==epoch && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))try{
                val service=future.get()
                if(!service.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)){state=MagnifierState(MagnifierStatus.UNAVAILABLE);return@addListener}
                val useCase=Preview.Builder().build();preview=useCase;provider=service
                useCase.setSurfaceProvider(view.surfaceProvider)
                // Permission is checked again immediately before binding, never inferred from an old request.
                if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){cleanup();state=MagnifierState(MagnifierStatus.PERMISSION_REQUIRED);return@addListener}
                val bound=service.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,useCase);camera=bound
                val observer=Observer<CameraState>{ value ->
                    if(alive && token==epoch && camera===bound && value.error!=null){cleanup();state=MagnifierState(MagnifierStatus.ERROR)}
                }
                cameraObserver=observer;bound.cameraInfo.cameraState.observe(owner,observer)
                if(token!=epoch)return@addListener
                val zoom=bound.cameraInfo.zoomState.value
                state=MagnifierState(MagnifierStatus.LIVE,zoom=zoom?.zoomRatio?:1f,minZoom=zoom?.minZoomRatio?:1f,maxZoom=zoom?.maxZoomRatio?:1f,torchAvailable=bound.cameraInfo.hasFlashUnit())
            }catch(_:Exception){cleanup();state=MagnifierState(MagnifierStatus.ERROR)}
        },ContextCompat.getMainExecutor(context))
    }
    DisposableEffect(owner,view){
        alive=true
        val observer=LifecycleEventObserver{_,_->if(!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))cleanup()}
        owner.lifecycle.addObserver(observer)
        onDispose{alive=false;owner.lifecycle.removeObserver(observer);cleanup()}
    }
    return MagnifierHandle(state,view,::start,::cleanup,{
        if(alive && state.status==MagnifierStatus.LIVE && view.previewStreamState.value==PreviewView.StreamState.STREAMING && validMagnifierDimensions(view.width,view.height)){
            val bitmap=runCatching{view.bitmap}.getOrNull()
            if(bitmap!=null && validMagnifierDimensions(bitmap.width,bitmap.height)){
                cleanup()
                state=MagnifierState(status=MagnifierStatus.FROZEN,frozen=bitmap)
            }else{bitmap?.recycle();cleanup();state=MagnifierState(MagnifierStatus.ERROR)}
        }
    },{if(state.status==MagnifierStatus.FROZEN)start()}, {value->
        val bound=camera;val token=epoch
        if(alive && state.status==MagnifierStatus.LIVE && bound!=null && value.isFinite()){
            val chosen=value.coerceIn(state.minZoom,state.maxZoom)
            val request=++zoomRequest
            val future=bound.cameraControl.setZoomRatio(chosen)
            future.addListener({if(alive && token==epoch && request==zoomRequest && camera===bound && state.status==MagnifierStatus.LIVE)try{future.get();state=state.copy(zoom=chosen)}catch(_:Exception){cleanup();state=MagnifierState(MagnifierStatus.ERROR)}},ContextCompat.getMainExecutor(context))
        }
    },{
        val bound=camera;val token=epoch
        if(alive && state.status==MagnifierStatus.LIVE && state.torchAvailable && bound!=null){
            val request=++torchRequest
            val enabled=!state.torch;val future=bound.cameraControl.enableTorch(enabled)
            future.addListener({if(alive && token==epoch && request==torchRequest && camera===bound && state.status==MagnifierStatus.LIVE)try{future.get();state=state.copy(torch=enabled)}catch(_:Exception){cleanup();state=MagnifierState(MagnifierStatus.ERROR)}},ContextCompat.getMainExecutor(context))
        }
    })
}
