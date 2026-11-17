package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import com.jaguar.gearbox.images.*
import com.jaguar.gearbox.ui.components.CompactChoiceSelector

@Composable
internal fun StudioEnhancementControls(brightness:String, contrast:String, saturation:String, enabled:Boolean,
    onBrightness:(String)->Unit, onContrast:(String)->Unit, onSaturation:(String)->Unit) {
    listOf(Triple(brightness,R.string.studio_brightness,onBrightness), Triple(contrast,R.string.studio_contrast,onContrast),
        Triple(saturation,R.string.studio_saturation,onSaturation)).forEach { (value,label,change) ->
        // Text keyboard intentionally provides a minus sign for negative adjustments.
        OutlinedTextField(value,change,enabled=enabled,singleLine=true,label={ Text(stringResource(label)) },modifier=Modifier.fillMaxWidth())
    }
    Text(stringResource(R.string.studio_enhancement_note),style=MaterialTheme.typography.bodySmall)
}

@Composable
internal fun StudioCollageChoices(layout:StudioCollageLayout,fit:StudioImageFit,background:String,enabled:Boolean,
    onLayout:(StudioCollageLayout)->Unit,onFit:(StudioImageFit)->Unit,onBackground:(String)->Unit) {
    val context=LocalContext.current
    CompactChoiceSelector(stringResource(R.string.studio_collage_layout),layout,StudioCollageLayout.entries,
        { context.getString(when(it) { StudioCollageLayout.HORIZONTAL->R.string.studio_horizontal; StudioCollageLayout.VERTICAL->R.string.studio_vertical; StudioCollageLayout.GRID->R.string.studio_grid }) },
        { if(enabled) onLayout(it) })
    CompactChoiceSelector(stringResource(R.string.studio_fit),fit,StudioImageFit.entries,
        { context.getString(if(it==StudioImageFit.CONTAIN) R.string.studio_contain else R.string.studio_cover) },{if(enabled) onFit(it)})
    OutlinedTextField(background,onBackground,enabled=enabled,singleLine=true,label={Text(stringResource(R.string.studio_background))},modifier=Modifier.fillMaxWidth())
}
