package com.jaguar.gearbox.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.camera.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

// Shared across screen instances: a disposed decoder must finish wiping before another can copy.
private val scannerDecodeLock=ReentrantLock()


class BarcodeScannerHandle(val state:BarcodeScannerState,val previewView:PreviewView,val start:()->Unit,val stop:()->Unit,val clear:()->Unit)
@Composable fun rememberBarcodeScanner():BarcodeScannerHandle {
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    val view=remember(context){PreviewView(context).apply{implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FIT_CENTER}}
    var state by remember{mutableStateOf(BarcodeScannerState())}
    val epoch=remember{AtomicLong()};val lock=scannerDecodeLock
    val executor=remember{Executors.newSingleThreadExecutor()};val main=remember(context){ContextCompat.getMainExecutor(context)}
    var alive by remember{mutableStateOf(true)}
    var provider by remember{mutableStateOf<ProcessCameraProvider?>(null)}
    var preview by remember{mutableStateOf<Preview?>(null)}
    var analysis by remember{mutableStateOf<ImageAnalysis?>(null)}
    var camera by remember{mutableStateOf<Camera?>(null)}
    var observer by remember{mutableStateOf<Observer<CameraState>?>(null)}
    fun cleanup(){
        epoch.incrementAndGet()
        val oldCamera=camera;val oldObserver=observer;val oldAnalysis=analysis;val oldPreview=preview;val service=provider
        camera=null;observer=null;analysis=null;preview=null;provider=null
        runCatching{if(oldObserver!=null)oldCamera?.cameraInfo?.cameraState?.removeObserver(oldObserver)}
        runCatching{oldAnalysis?.clearAnalyzer()}
        runCatching{if(oldAnalysis!=null)service?.unbind(oldAnalysis)}
        runCatching{if(oldPreview!=null)service?.unbind(oldPreview)}
        state=BarcodeScannerState()
    }
    fun start(){
        if(!alive || !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))return
        cleanup()
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){state=BarcodeScannerState(BarcodeScannerStatus.PERMISSION_REQUIRED);return}
        val token=epoch.get();state=BarcodeScannerState(BarcodeScannerStatus.STARTING)
        val future=ProcessCameraProvider.getInstance(context)
        future.addListener({
            if(alive && token==epoch.get() && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))try{
                val service=future.get()
                if(!service.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)){state=BarcodeScannerState(BarcodeScannerStatus.UNAVAILABLE);return@addListener}
                val usePreview=Preview.Builder().build();preview=usePreview;provider=service;usePreview.setSurfaceProvider(view.surfaceProvider)
                val useAnalysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setTargetResolution(Size(640,480)).build();analysis=useAnalysis
                useAnalysis.setAnalyzer(executor){proxy->
                    var bytes:ByteArray?=null;var held=false
                    try{
                        if(token==epoch.get() && lock.tryLock()){
                            held=true
                            if(token==epoch.get()){
                                val plane=proxy.planes.firstOrNull()
                                if(plane!=null)bytes=scannerLuminance(plane.buffer,proxy.width,proxy.height,plane.rowStride,plane.pixelStride)
                                val payload=bytes
                                if(token==epoch.get() && payload!=null){
                                    val result=decodeScannerFrame(payload,proxy.width,proxy.height)
                                    if(result!=null)main.execute{
                                        if(alive && token==epoch.get() && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)){
                                            cleanup();state=if(result.text.length>4096)BarcodeScannerState(BarcodeScannerStatus.TOO_LARGE) else BarcodeScannerState(BarcodeScannerStatus.RESULT,result)
                                        }
                                    }
                                }
                            }
                        }
                    }catch(_:Exception){/* A bad frame is discarded, never interpreted as a result. */}
                    finally{bytes?.fill(0);if(held)lock.unlock();proxy.close()}
                }
                if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){cleanup();state=BarcodeScannerState(BarcodeScannerStatus.PERMISSION_REQUIRED);return@addListener}
                val bound=service.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,usePreview,useAnalysis);camera=bound
                val statusObserver=Observer<CameraState>{value->if(alive && token==epoch.get() && value.error!=null){cleanup();state=BarcodeScannerState(BarcodeScannerStatus.ERROR)}}
                observer=statusObserver;bound.cameraInfo.cameraState.observe(owner,statusObserver)
                if(token==epoch.get())state=BarcodeScannerState(BarcodeScannerStatus.SCANNING)
            }catch(_:Exception){cleanup();state=BarcodeScannerState(BarcodeScannerStatus.ERROR)}
        },main)
    }
    DisposableEffect(owner,view){
        alive=true
        val lifecycleObserver=LifecycleEventObserver{_,_->if(!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))cleanup()}
        owner.lifecycle.addObserver(lifecycleObserver)
        onDispose{alive=false;owner.lifecycle.removeObserver(lifecycleObserver);cleanup();executor.shutdown()}
    }
    return BarcodeScannerHandle(state,view,::start,::cleanup,::cleanup)
}
