package com.jaguar.gearbox.ui.tools
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jaguar.gearbox.R
import com.jaguar.gearbox.camera.*
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.*

@Composable fun BarcodeScannerScreen(onNavigateBack:()->Unit){
    val context=LocalContext.current;val scanner=rememberBarcodeScanner();val latest by rememberUpdatedState(scanner.state)
    var active by remember{mutableStateOf(true)};var notice by remember{mutableStateOf(false)};var openFailed by remember{mutableStateOf(false)}
    var shareReview by remember{mutableStateOf<ScannedBarcode?>(null)};var openReview by remember{mutableStateOf<ScannedBarcode?>(null)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(active)notice=true}
    DisposableEffect(Unit){onDispose{active=false;shareReview=null;openReview=null}}
    val result=scanner.state.result
    ToolScaffold(stringResource(R.string.scanner_title),Tools.byRoute(Tools.ROUTE_BARCODE_SCANNER)!!.icon,onNavigateBack){
        AdaptiveToolLayout(input={
            Text(stringArrayResource(R.array.scanner_statuses)[scanner.state.status.ordinal])
            if(notice)Text(stringResource(R.string.scanner_permission_notice))
            Button(enabled=scanner.state.status !in listOf(BarcodeScannerStatus.STARTING,BarcodeScannerStatus.SCANNING),onClick={
                notice=false;shareReview=null;openReview=null
                if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)scanner.start() else permission.launch(Manifest.permission.CAMERA)
            }){Text(stringResource(R.string.scanner_start))}
            OutlinedButton(onClick={shareReview=null;openReview=null;scanner.stop()}){Text(stringResource(R.string.scanner_stop))}
            if(scanner.state.status in listOf(BarcodeScannerStatus.STARTING,BarcodeScannerStatus.SCANNING)){
                val density=LocalDensity.current.density
                AndroidView(factory={scanner.previewView},modifier=Modifier.size((640f/density).dp))
            }
            Text(stringResource(R.string.scanner_warning));Text(stringResource(R.string.scanner_privacy))
        },result={
            if(result!=null){
                Text(stringResource(R.string.scanner_format,result.format));Text(result.text)
                ToolActions(onCopy={if(active && latest.result===result)context.copyToClipboard(context.getString(R.string.scanner_title),result.text,sensitive=true)},
                    onShare={if(active && latest.result===result)shareReview=result},onClear={shareReview=null;openReview=null;scanner.clear()},resultEnabled=true,clearEnabled=true)
                if(safeScannerHttpUrl(result.text)!=null)TextButton(onClick={if(active && latest.result===result)openReview=result}){Text(stringResource(R.string.scanner_open))}
            }
            if(openFailed)Text(stringResource(R.string.scanner_open_failed))
        })
    }
    shareReview?.let{reviewed->
        if(latest.result!==reviewed)LaunchedEffect(reviewed){shareReview=null}
        else SensitiveTextShareDialog(reviewed.text,onConfirm={
            val valid=active && shareReview===reviewed && latest.result===reviewed;shareReview=null
            if(valid)context.shareText(reviewed.text)
        },onDismiss={shareReview=null})
    }
    openReview?.let{reviewed->
        val url=safeScannerHttpUrl(reviewed.text);val host=scannerUrlHost(reviewed.text)
        if(latest.result!==reviewed || url==null || host==null)LaunchedEffect(reviewed){openReview=null}
        else AlertDialog(onDismissRequest={openReview=null},title={Text(stringResource(R.string.scanner_open))},
            text={Text(stringResource(R.string.scanner_open_review,host,url))},
            confirmButton={TextButton(onClick={
                val valid=active && openReview===reviewed && latest.result===reviewed && safeScannerHttpUrl(reviewed.text)==url;openReview=null
                if(valid)try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}catch(_:Exception){openFailed=true}
            }){Text(stringResource(R.string.scanner_open))}},dismissButton={TextButton(onClick={openReview=null}){Text(stringResource(android.R.string.cancel))}})
    }
}
