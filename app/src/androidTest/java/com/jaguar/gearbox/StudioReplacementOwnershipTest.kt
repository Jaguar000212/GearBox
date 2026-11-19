package com.jaguar.gearbox

import android.graphics.Bitmap
import android.net.Uri
import com.jaguar.gearbox.images.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class StudioReplacementOwnershipTest {
    private fun image() = ImageStudioImage(Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888),4,4)
    @Test fun failedBatchRecyclesOnlyUnpublishedImages() = runBlocking {
        val existing=image();val loaded=image();var calls=0
        try {
            try {
                loadStudioReplacement(listOf(Uri.EMPTY,Uri.EMPTY),1_000_000L){_,_->
                    if(calls++==0)loaded else error("decode failure")
                }
                fail("Expected failure")
            }catch(_:IllegalStateException){}
            assertTrue(loaded.bitmap.isRecycled)
            assertFalse(existing.bitmap.isRecycled)
        }finally{existing.bitmap.recycle()}
    }
    @Test fun cancelledBatchDiscardsPartialLoadAndSuccessfulBatchTransfersOwnership() = runBlocking {
        val loaded=image();var calls=0
        try {
            loadStudioReplacement(listOf(Uri.EMPTY,Uri.EMPTY),1_000_000L){_,_->
                if(calls++==0)loaded else throw CancellationException("test cancellation")
            }
            fail("Expected cancellation")
        }catch(_:CancellationException){}
        assertTrue(loaded.bitmap.isRecycled)
        val one=image();val two=image();var count=0
        val result=loadStudioReplacement(listOf(Uri.EMPTY,Uri.EMPTY),1_000_000L){_,_->if(count++==0)one else two}
        try {assertEquals(listOf(one,two),result);assertFalse(one.bitmap.isRecycled);assertFalse(two.bitmap.isRecycled)}
        finally{one.bitmap.recycle();two.bitmap.recycle()}
    }
}
