package com.jaguar.gearbox

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import com.jaguar.gearbox.images.ImageStudioImage
import com.jaguar.gearbox.ui.tools.ImageStudioScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ImageStudioReplacementTest {
    @get:Rule val compose=createComposeRule()
    private val isolated=IsolatedToolTestContext()
    private val context=object:ContextWrapper(isolated){
        override fun getApplicationContext():Context=this
        override fun getCacheDir()=File(isolated.noBackupFilesDir,"replacement-cache").also{it.mkdirs()}
    }
    private class Registry:ActivityResultRegistry(){
        var code:Int?=null;var fail=false
        override fun <I,O> onLaunch(requestCode:Int,contract:ActivityResultContract<I,O>,input:I,options:ActivityOptionsCompat?){
            if(fail)throw IllegalStateException("picker unavailable")
            code=requestCode
        }
        fun cancel(){dispatchResult(requireNotNull(code),Activity.RESULT_CANCELED,null);code=null}
        fun oneInvalidUri(){dispatchResult(requireNotNull(code),Activity.RESULT_OK,Intent().setData(Uri.parse("content://gearbox.invalid/missing")));code=null}
    }
    private val registry=Registry()
    private val owner=object:ActivityResultRegistryOwner{override val activityResultRegistry=registry}
    @After fun cleanup(){isolated.cleanup()}
    private fun open(collage:Boolean=false, loader:(suspend (Uri,Long)->ImageStudioImage)?=null){
        val image=ImageStudioImage(Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888),8,8)
        val other=ImageStudioImage(Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888),8,8)
        compose.setContent{CompositionLocalProvider(LocalContext provides context,LocalActivityResultRegistryOwner provides owner){
            MaterialTheme{ImageStudioScreen({},if(collage)null else image,if(collage)listOf(image,other) else emptyList(),loader)}
        }}
        if(collage){
            compose.onNodeWithText(context.getString(R.string.studio_width)).performScrollTo().performTextReplacement("16")
            compose.onNodeWithText(context.getString(R.string.studio_height)).performScrollTo().performTextReplacement("8")
        }else{
            compose.onNodeWithText(context.getString(R.string.studio_markup_show)).performScrollTo().performClick()
            compose.onNodeWithText(context.getString(R.string.studio_markup_add)).performScrollTo().performClick()
        }
        compose.onNodeWithText(context.getString(R.string.studio_generate)).performScrollTo().performClick()
        compose.waitUntil(10000){compose.onAllNodesWithContentDescription(context.getString(R.string.studio_output_preview)).fetchSemanticsNodes().isNotEmpty()}
    }
    private fun previewRemains(){compose.onNodeWithContentDescription(context.getString(R.string.studio_output_preview)).assertExists()}
    @Test fun singleCancelLaunchFailureAndDecodeFailurePreserveEditsAndPreview(){
        open()
        compose.onNodeWithText(context.getString(R.string.studio_choose)).performScrollTo().performClick()
        previewRemains();compose.runOnIdle{registry.cancel()};previewRemains()
        compose.onNodeWithText(context.getString(R.string.studio_markup_count,1)).assertExists()
        compose.runOnIdle{registry.fail=true}
        compose.onNodeWithText(context.getString(R.string.studio_choose)).performScrollTo().performClick();previewRemains()
        compose.runOnIdle{registry.fail=false}
        compose.onNodeWithText(context.getString(R.string.studio_choose)).performScrollTo().performClick()
        compose.runOnIdle{registry.oneInvalidUri()}
        compose.waitUntil(10000){compose.onAllNodesWithText(context.getString(R.string.studio_failed)).fetchSemanticsNodes().isNotEmpty()}
        previewRemains();compose.onNodeWithText(context.getString(R.string.studio_markup_count,1)).assertExists()
    }
    @Test fun collageCancelAndInvalidCountPreserveExistingImagesAndPreview(){
        open(true)
        compose.onNodeWithText(context.getString(R.string.studio_choose_collage)).performScrollTo().performClick()
        compose.runOnIdle{registry.cancel()};previewRemains()
        compose.onNodeWithText(context.getString(R.string.studio_choose_collage)).performScrollTo().performClick()
        compose.runOnIdle{registry.oneInvalidUri()};previewRemains()
        compose.onNodeWithText(context.getString(R.string.studio_collage_count,2)).assertExists()
        compose.onNodeWithText(context.getString(R.string.studio_collage_invalid)).assertExists()
    }
    @Test fun successfulReplacementClearsOldOutputOnlyAfterLoadCompletes(){
        val ready=kotlinx.coroutines.CompletableDeferred<Unit>()
        val replacement=ImageStudioImage(Bitmap.createBitmap(12,6,Bitmap.Config.ARGB_8888),12,6)
        open(loader={_,_->ready.await();replacement})
        compose.onNodeWithText(context.getString(R.string.studio_choose)).performScrollTo().performClick()
        compose.runOnIdle{registry.oneInvalidUri()}
        previewRemains()
        compose.onNodeWithText(context.getString(R.string.studio_markup_count,1)).assertExists()
        compose.runOnIdle{ready.complete(Unit)}
        compose.waitUntil(10000){compose.onAllNodesWithContentDescription(context.getString(R.string.studio_output_preview)).fetchSemanticsNodes().isEmpty()}
        compose.onNodeWithText(context.getString(R.string.studio_source,12,6,12,6)).assertExists()
        compose.onNodeWithText(context.getString(R.string.studio_markup_count,0)).assertExists()
        assertFalse(replacement.bitmap.isRecycled)
    }

}
