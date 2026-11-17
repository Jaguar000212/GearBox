package com.jaguar.gearbox.ui.tools

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.images.*
import com.jaguar.gearbox.ui.components.*
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

private data class StudioOutput(val image: Bitmap, val encoded: ImageStudioExport, val format: ImageStudioFormat, val generation: Long)
private data class StudioReview(val output: StudioOutput, val share: Boolean)
private fun studioFormatLabel(format: ImageStudioFormat): Int = when (format) {
    ImageStudioFormat.PNG -> R.string.studio_png
    ImageStudioFormat.JPEG -> R.string.studio_jpeg
    ImageStudioFormat.WEBP -> R.string.studio_webp
}

/** initialImage is an internal test seam; normal navigation always starts without an image. */
@Composable
fun ImageStudioScreen(onNavigateBack: () -> Unit, initialImage: ImageStudioImage? = null, initialCollage: List<ImageStudioImage> = emptyList()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(true) }
    var generation by remember { mutableLongStateOf(0) }
    var collageMode by remember { mutableStateOf(initialCollage.isNotEmpty()) }
    var collageImages by remember { mutableStateOf(initialCollage) }
    var collageWidth by remember { mutableStateOf("1024") }
    var collageHeight by remember { mutableStateOf("1024") }
    var collageLayout by remember { mutableStateOf(StudioCollageLayout.GRID) }
    var collageFit by remember { mutableStateOf(StudioImageFit.CONTAIN) }
    var background by remember { mutableStateOf("#FFFFFF") }
    var brightness by remember { mutableStateOf("0") }
    var contrast by remember { mutableStateOf("0") }
    var saturation by remember { mutableStateOf("0") }
    var source by remember { mutableStateOf(initialImage) }
    var edits by remember { mutableStateOf<List<ImageStudioEdit>>(emptyList()) }
    var output by remember { mutableStateOf<StudioOutput?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var pickGeneration by remember { mutableLongStateOf(0) }
    var review by remember { mutableStateOf<StudioReview?>(null) }
    var pendingSave by remember { mutableStateOf<StudioOutput?>(null) }
    var message by remember { mutableStateOf<Int?>(null) }
    var left by remember { mutableStateOf("0") }
    var top by remember { mutableStateOf("0") }
    var right by remember { mutableStateOf(initialImage?.bitmap?.width?.toString().orEmpty()) }
    var bottom by remember { mutableStateOf(initialImage?.bitmap?.height?.toString().orEmpty()) }
    var width by remember { mutableStateOf(initialImage?.bitmap?.width?.toString().orEmpty()) }
    var height by remember { mutableStateOf(initialImage?.bitmap?.height?.toString().orEmpty()) }
    var format by remember { mutableStateOf(ImageStudioFormat.PNG) }
    var quality by remember { mutableStateOf("90") }
    var aspectLock by remember { mutableStateOf(true) }
    fun invalidate() {
        generation++; job?.cancel(); job = null; busy = false
        output = null; review = null; pendingSave = null; message = null
    }
    fun install(image: ImageStudioImage) {
        edits = emptyList(); source = image; left = "0"; top = "0"; right = image.bitmap.width.toString(); bottom = image.bitmap.height.toString()
        width = image.bitmap.width.toString(); height = image.bitmap.height.toString()
    }
    rememberImageStudioModes(mapOf("format" to if (format == ImageStudioFormat.WEBP) "WEBP_LOSSY" else format.name,
        "quality" to quality, "aspectLock" to aspectLock.toString()), onRestoreMode = { saved ->
        // A late preference read must not cancel a pick, transform or export already requested.
        if (generation == 0L) {
            format = if (saved["format"] == "WEBP_LOSSY") ImageStudioFormat.WEBP else ImageStudioFormat.valueOf(saved.getValue("format"))
            quality = saved.getValue("quality"); aspectLock = saved.getValue("aspectLock").toBooleanStrict()
        }
    })
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val wasPicking = picking
        picking = false
        if (uri != null && wasPicking && active && generation == pickGeneration) {
            val token = generation; busy = true
            job = scope.launch {
                try {
                    val loaded = loadStudioImage(context.contentResolver, uri)
                    if (active && token == generation) install(loaded) else loaded.bitmap.recycle()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { if (active && token == generation) message = R.string.studio_failed }
                catch (_: OutOfMemoryError) { if (active && token == generation) message = R.string.studio_failed }
                finally { if (active && token == generation) busy = false }
            }
        }
    }
    val collagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { uris ->
        val wasPicking = picking; picking = false
        if (wasPicking && active && generation == pickGeneration && collageMode && uris.isNotEmpty()) {
            if (uris.size !in 2..4) message = R.string.studio_collage_invalid else {
                val token = generation; busy = true
                job = scope.launch {
                    val loaded = mutableListOf<ImageStudioImage>()
                    var published = false
                    try {
                        for (uri in uris) loaded += loadStudioImage(context.contentResolver, uri, maxWorkingPixels = 1_000_000L)
                        if (active && token == generation && collageMode) {
                            collageImages = loaded.toList(); published = true
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { if (active && token == generation) message = R.string.studio_failed }
                    catch (_: OutOfMemoryError) { if (active && token == generation) message = R.string.studio_failed }
                    finally { if (!published) loaded.forEach { it.bitmap.recycle() }; if (active && token == generation) busy = false }
                }
            }
        }
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        val snapshot = pendingSave; pendingSave = null
        val uri = response.data?.data
        if (response.resultCode == Activity.RESULT_OK && uri != null) {
            if (!active || snapshot == null || output !== snapshot || snapshot.generation != generation) {
                if (active) message = R.string.studio_stale
            } else {
                saving = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            snapshot.encoded.file.inputStream().use { input ->
                                (context.contentResolver.openOutputStream(uri, "w") ?: error("No destination stream")).use { destination ->
                                    val buffer = ByteArray(8192)
                                    while (true) {
                                        coroutineContext.ensureActive()
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        destination.write(buffer, 0, count)
                                    }
                                }
                            }
                        }
                        if (active && output === snapshot && generation == snapshot.generation) message = R.string.studio_saved
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { if (active) message = R.string.studio_failed }
                    finally { if (active) saving = false }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { active = false; generation++; job?.cancel(); pendingSave = null; review = null }
    }
    fun parsedUnsigned(text: String): Int? = text.takeIf { Regex("0|[1-9][0-9]{0,4}").matches(it) }?.toIntOrNull()
    val crop = listOf(left, top, right, bottom).map(::parsedUnsigned).let { values ->
        if (values.all { it != null }) ImageStudioCrop(values[0]!!, values[1]!!, values[2]!!, values[3]!!) else null
    }
    val enhancement = listOf(brightness, contrast, saturation).map { it.toIntOrNull()?.takeIf { n -> n in -100..100 } }.let {
        if (it.all { value -> value != null }) StudioEnhancement(it[0]!!, it[1]!!, it[2]!!) else null
    }
    val backgroundColor = background.takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) }?.drop(1)?.toLongOrNull(16)?.or(0xff000000L)?.toInt()
    val targetWidth = parsedUnsigned(if (collageMode) collageWidth else width)
    val cropValid = source?.let { crop?.validFor(it.bitmap.width, it.bitmap.height) } == true
    val targetHeight = if (!collageMode && aspectLock && cropValid && targetWidth != null && crop != null) {
        val numerator = targetWidth.toLong() * (crop.bottom - crop.top)
        ((numerator * 2 + (crop.right - crop.left)) / (2L * (crop.right - crop.left))).takeIf { it <= Int.MAX_VALUE }?.toInt()
    } else if (collageMode || !aspectLock) parsedUnsigned(if (collageMode) collageHeight else height) else null
    val qualityValue = if (format == ImageStudioFormat.PNG) 90 else parsedUnsigned(quality)?.takeIf { it in 1..100 }
    val collageSizeValid = targetWidth != null && targetHeight != null &&
        runCatching { studioCollageCells(collageImages.size,targetWidth,targetHeight,collageLayout) }.isSuccess
    val canGenerate = (if (collageMode) collageImages.size in 2..4 && backgroundColor != null && collageSizeValid else source != null && cropValid) && targetWidth != null && targetHeight != null &&
        ImageStudioLimits.validOutput(targetWidth, targetHeight) && qualityValue != null && enhancement != null && !busy && !saving && !picking
    ToolScaffold(stringResource(R.string.studio_title), Tools.byRoute(Tools.ROUTE_IMAGE_STUDIO)!!.icon, onNavigateBack) {
        CompactChoiceSelector(stringResource(R.string.studio_mode), collageMode, listOf(false,true),
            { context.getString(if(it) R.string.studio_collage else R.string.studio_single) },
            { if (!saving && !picking && collageMode != it) { invalidate(); collageMode = it

            } })
        if (collageMode) OutlinedButton(enabled = !saving && !picking, onClick = {
            invalidate(); collageImages = emptyList(); picking = true; pickGeneration = generation
            runCatching { collagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                .onFailure { picking = false; message = R.string.studio_failed }
        }) { Text(stringResource(R.string.studio_choose_collage)) }
        else OutlinedButton(enabled = !saving && !picking, onClick = {
            invalidate(); edits = emptyList(); source = null; picking = true; pickGeneration = generation
            runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                .onFailure { picking = false; message = R.string.studio_failed }
        }) { Text(stringResource(R.string.studio_choose)) }
        Text(stringResource(R.string.studio_disclosure), style = MaterialTheme.typography.bodySmall)
        (if (collageMode) collageImages.firstOrNull() else source)?.let { image ->
            AdaptiveToolLayout(input = {
                if (collageMode) {
                    Text(stringResource(R.string.studio_collage_count, collageImages.size))
                    collageImages.forEachIndexed { index, item -> Image(item.bitmap.asImageBitmap(),
                        stringResource(R.string.studio_collage_preview,index+1),contentScale=ContentScale.Fit,
                        modifier=Modifier.fillMaxWidth().heightIn(max=140.dp)) }
                    StudioCollageChoices(collageLayout,collageFit,background,!saving,
                        {collageLayout=it;invalidate()},{collageFit=it;invalidate()},{background=it;invalidate()})
                    Text(stringResource(R.string.studio_collage_note),style=MaterialTheme.typography.bodySmall)
                } else {
                Text(stringResource(R.string.studio_source, image.bitmap.width, image.bitmap.height, image.originalWidth, image.originalHeight))
                Image(image.bitmap.asImageBitmap(), stringResource(R.string.studio_source_preview),
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp))
                ImageStudioMarkup(image.bitmap.width, image.bitmap.height, edits, !saving,
                    onDraftChanged = { invalidate() }, onEditsChanged = { edits = it; invalidate() })
                StudioNumber(left, { left = it; invalidate() }, R.string.studio_left, !saving)
                StudioNumber(top, { top = it; invalidate() }, R.string.studio_top, !saving)
                StudioNumber(right, { right = it; invalidate() }, R.string.studio_right, !saving)
                StudioNumber(bottom, { bottom = it; invalidate() }, R.string.studio_bottom, !saving)
                Text(stringResource(R.string.studio_crop_note), style = MaterialTheme.typography.bodySmall)
                }
                StudioNumber(if(collageMode) collageWidth else width, { if(collageMode) collageWidth = it else width = it; invalidate() }, R.string.studio_width, !saving)
                StudioNumber(if (!collageMode && aspectLock) targetHeight?.toString().orEmpty() else if(collageMode) collageHeight else height,
                    { if(collageMode) collageHeight = it else height = it; invalidate() }, R.string.studio_height, !saving && (collageMode || !aspectLock))
                if (!collageMode) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.studio_lock), modifier = Modifier.weight(1f))
                    val lockLabel = stringResource(R.string.studio_lock)
                    Switch(checked = aspectLock, enabled = !saving, modifier = Modifier.semantics { contentDescription = lockLabel }, onCheckedChange = {
                        if (!it && targetHeight != null) height = targetHeight.toString()
                        aspectLock = it; invalidate()
                    })
                }
                if (!collageMode) Text(stringResource(R.string.studio_lock_note), style = MaterialTheme.typography.bodySmall)
                StudioEnhancementControls(brightness,contrast,saturation,!saving,
                    {brightness=it;invalidate()},{contrast=it;invalidate()},{saturation=it;invalidate()})
                CompactChoiceSelector(stringResource(R.string.studio_format), format, ImageStudioFormat.entries,
                    { context.getString(studioFormatLabel(it)) }, { if (!saving) { format = it; invalidate() } })
                if (format != ImageStudioFormat.PNG) StudioNumber(quality, { quality = it; invalidate() }, R.string.studio_quality, !saving)
                if (!canGenerate && !busy && !saving) Text(stringResource(if(collageMode) R.string.studio_collage_invalid else R.string.studio_invalid), color = MaterialTheme.colorScheme.error)
                Button(enabled = canGenerate, onClick = {
                    val selectedCrop = crop
                    val selectedEnhancement = enhancement ?: return@Button
                    val selectedCollage = collageMode; val selectedSources = collageImages.map { it.bitmap }
                    val selectedLayout = collageLayout; val selectedFit = collageFit; val selectedBackground = backgroundColor
                    val w = targetWidth ?: return@Button; val h = targetHeight ?: return@Button; val q = qualityValue ?: return@Button
                    invalidate(); val token = generation; val selectedFormat = format; val selectedEdits = edits.toList()
                    busy = true
                    job = scope.launch {
                        var transformed: Bitmap? = null
                        var preview: Bitmap? = null
                        try {
                            transformed = if (selectedCollage) collageStudioImages(selectedSources,w,h,selectedLayout,selectedFit,
                                requireNotNull(selectedBackground),selectedEnhancement)
                            else transformStudioImage(image.bitmap, requireNotNull(selectedCrop), w, h, selectedEdits,selectedEnhancement)
                            val encoded = exportStudioImage(context.cacheDir, requireNotNull(transformed), selectedFormat, q)
                            // Keep ownership before crossing the cancellation boundary back to Main.
                            withContext(Dispatchers.IO) { preview = BitmapFactory.decodeFile(encoded.file.absolutePath) ?: error("No encoded preview") }
                            if (active && generation == token) {
                                output = StudioOutput(requireNotNull(preview), encoded, selectedFormat, token); preview = null
                            }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { if (active && token == generation) message = R.string.studio_failed }
                        catch (_: OutOfMemoryError) { if (active && token == generation) message = R.string.studio_failed }
                        finally { transformed?.recycle(); preview?.recycle(); if (active && token == generation) busy = false }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.studio_generate)) }
                if (busy) TextButton(onClick = { invalidate() }) { Text(stringResource(R.string.studio_cancel_work)) }
                TextButton(enabled = !saving, onClick = { invalidate(); if (collageMode) collageImages = emptyList() else { edits = emptyList(); source = null } }) { Text(stringResource(R.string.studio_remove)) }
            }, result = {
                output?.let { generated ->
                    Image(generated.image.asImageBitmap(), stringResource(R.string.studio_output_preview),
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp))
                    Text(stringResource(R.string.studio_review_info, generated.image.width, generated.image.height,
                        context.getString(studioFormatLabel(generated.format)), generated.encoded.file.length()))
                    OutlinedButton(enabled = !saving && pendingSave == null, onClick = { review = StudioReview(generated, false) }) { Text(stringResource(R.string.studio_save)) }
                    OutlinedButton(enabled = !saving && pendingSave == null, onClick = { review = StudioReview(generated, true) }) { Text(stringResource(R.string.studio_share)) }
                }
                Text(stringResource(R.string.studio_encoding), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.studio_lifetime), style = MaterialTheme.typography.bodySmall)
            })
        } ?: Text(stringResource(R.string.studio_no_source))
        if (busy && (if (collageMode) collageImages.isEmpty() else source == null)) TextButton(onClick = { invalidate() }) { Text(stringResource(R.string.studio_cancel_work)) }
        if (busy || saving) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.studio_busy)) }
        message?.let { Text(stringResource(it)) }
    }
    review?.let { request ->
        val snapshot = request.output
        AlertDialog(onDismissRequest = { review = null },
            title = { Text(stringResource(if (request.share) R.string.studio_review_share else R.string.studio_review_save)) },
            text = { Column {
                Image(snapshot.image.asImageBitmap(), stringResource(R.string.studio_output_preview), contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp))
                Text(stringResource(R.string.studio_review_info, snapshot.image.width, snapshot.image.height,
                    context.getString(studioFormatLabel(snapshot.format)), snapshot.encoded.file.length()))
                Text(stringResource(if (request.share) R.string.studio_share_note else R.string.studio_save_note))
            } },
            confirmButton = { TextButton(onClick = {
                val current = active && review === request && output === snapshot && generation == snapshot.generation && !saving
                review = null
                if (current) {
                    if (request.share) runCatching {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", snapshot.encoded.file)
                        val send = Intent(Intent.ACTION_SEND).setType(snapshot.encoded.mimeType).putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri("Image", uri) }
                        context.startActivity(Intent.createChooser(send, context.getString(R.string.studio_share)))
                    }.onFailure { message = R.string.studio_failed }
                    else {
                        pendingSave = snapshot
                        runCatching { saveLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                            .setType(snapshot.encoded.mimeType).putExtra(Intent.EXTRA_TITLE, "GearBox-image-${System.currentTimeMillis()}.${snapshot.format.extension}")) }
                            .onFailure { pendingSave = null; message = R.string.studio_failed }
                    }
                }
            }) { Text(stringResource(if (request.share) R.string.studio_continue_share else R.string.studio_continue_save)) } },
            dismissButton = { TextButton(onClick = { review = null }) { Text(stringResource(R.string.studio_cancel)) } })
    }
}

@Composable
private fun StudioNumber(value: String, onChange: (String) -> Unit, label: Int, enabled: Boolean) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = true, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
}
