package com.jaguar.gearbox.ui.tools
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jaguar.gearbox.R
import com.jaguar.gearbox.camera.*
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.*
import kotlinx.coroutines.*

@Composable fun MagnifierScreen(onNavigateBack:()->Unit){
    val context=LocalContext.current;val camera=rememberMagnifier();val latest by rememberUpdatedState(camera.state)
    val scope=rememberCoroutineScope();var active by remember{mutableStateOf(true)}
    var notice by remember{mutableStateOf(false)};var failure by remember{mutableStateOf(false)};var saving by remember{mutableStateOf(false)}
    var review by remember{mutableStateOf<Bitmap?>(null)};val pending=remember{MagnifierExportSelection()}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(active)notice=true}
    val destination=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val frame=pending.take();val uri=result.data?.data
        if(result.resultCode==android.app.Activity.RESULT_OK && uri!=null){
            if(!active || frame==null || uri.scheme!="content")failure=true
            else scope.launch{
                saving=true
                try{val ok=withContext(Dispatchers.IO){ensureActive();context.contentResolver.openOutputStream(uri,"w")?.use{writeMagnifierPng(frame,it)}==true};failure=!ok}
                catch(cancel:CancellationException){throw cancel}catch(_:Exception){failure=true}finally{saving=false}
            }
        }
    }
    DisposableEffect(Unit){onDispose{active=false;pending.clear();review=null}}
    ToolScaffold(stringResource(R.string.magnifier_title),Tools.byRoute(Tools.ROUTE_MAGNIFIER)!!.icon,onNavigateBack){
        AdaptiveToolLayout(input={
            Text(stringArrayResource(R.array.magnifier_statuses)[camera.state.status.ordinal])
            if(notice)Text(stringResource(R.string.magnifier_permission_notice))
            Button(enabled=!saving && camera.state.status !in listOf(MagnifierStatus.LIVE,MagnifierStatus.FROZEN,MagnifierStatus.STARTING),onClick={
                notice=false;failure=false
                if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)camera.start() else permission.launch(Manifest.permission.CAMERA)
            }){Text(stringResource(R.string.magnifier_start))}
            OutlinedButton(onClick={review=null;camera.stop()},enabled=!saving){Text(stringResource(R.string.magnifier_stop))}
            if(camera.state.status==MagnifierStatus.LIVE){
                if(camera.state.maxZoom>camera.state.minZoom) MagnifierZoomSlider(camera.state.zoom,camera.state.minZoom,camera.state.maxZoom,camera.zoom)
                Text(stringResource(R.string.magnifier_zoom,camera.state.zoom))
                if(camera.state.torchAvailable)TextButton(onClick=camera.torch){Text(stringResource(if(camera.state.torch)R.string.magnifier_torch_off else R.string.magnifier_torch_on))}
                Button(onClick=camera.freeze){Text(stringResource(R.string.magnifier_freeze))}
            }
            if(camera.state.status==MagnifierStatus.FROZEN){
                Button(onClick={review=null;camera.resume()},enabled=!saving){Text(stringResource(R.string.magnifier_resume))}
                Button(onClick={if(active && latest.frozen===camera.state.frozen)review=camera.state.frozen},enabled=!saving){Text(stringResource(R.string.magnifier_export))}
            }
            if(failure)Text(stringResource(R.string.magnifier_export_failed),color=MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.magnifier_warning));Text(stringResource(R.string.magnifier_privacy))
        },result={
            if(camera.state.status in listOf(MagnifierStatus.LIVE,MagnifierStatus.STARTING)){
                val density=LocalDensity.current.density
                AndroidView(factory={camera.previewView},modifier=Modifier.size((1000f/density).dp))
            }
            camera.state.frozen?.let{Image(it.asImageBitmap(),stringResource(R.string.magnifier_frozen),modifier=Modifier.fillMaxWidth())}
        })
    }
    review?.let{frame->
        if(latest.status!=MagnifierStatus.FROZEN || latest.frozen!==frame)LaunchedEffect(frame){review=null}
        else AlertDialog(onDismissRequest={review=null},title={Text(stringResource(R.string.magnifier_export))},
            text={Column{Image(frame.asImageBitmap(),stringResource(R.string.magnifier_frozen),modifier=Modifier.heightIn(max=200.dp));Text(stringResource(R.string.magnifier_export_review))}},
            confirmButton={TextButton(onClick={
                val valid=active && review===frame && latest.status==MagnifierStatus.FROZEN && latest.frozen===frame
                review=null
                if(valid && pending.select(frame))try{destination.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="image/png";putExtra(Intent.EXTRA_TITLE,"magnifier.png")})}catch(_:Exception){pending.clear();failure=true}
            }){Text(stringResource(R.string.magnifier_export))}},dismissButton={TextButton(onClick={review=null}){Text(stringResource(android.R.string.cancel))}})
    }
}

/** Keeps the adjustable control named independently of its adjacent visual label. */
@Composable
internal fun MagnifierZoomSlider(zoom: Float, minimum: Float, maximum: Float, onZoom: (Float) -> Unit) {
    val description = stringResource(R.string.magnifier_zoom, zoom)
    Slider(zoom, onValueChange = onZoom, valueRange = minimum..maximum,
        modifier = Modifier.semantics {
            contentDescription = description
            stateDescription = description
        })
}
