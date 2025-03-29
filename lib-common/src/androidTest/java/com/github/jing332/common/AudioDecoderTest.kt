package com.github.jing332.common

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drake.net.Net
import com.github.jing332.common.audio.AudioUtils
import com.github.jing332.common.audio.exo.ExoAudioDecoder
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FilterInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class AudioDecoderTest {

    class TestInputStream(ins: InputStream) : FilterInputStream(ins) {
        val random = Random(System.currentTimeMillis())
        fun randomThrowErr() {
//            if (random.nextBoolean())
//                throw RuntimeException("test throw error from stream")
        }

        override fun read(): Int {
            randomThrowErr()
            return super.read()
        }

        override fun read(b: ByteArray?, off: Int, len: Int): Int {
            randomThrowErr()
            return super.read(b, off, len)
        }

        override fun available(): Int {
            randomThrowErr()
            return super.available()
        }

    }

    private fun ByteBuffer.toBytes(): ByteArray {
        val bytes = ByteArray(remaining())
        this.get(bytes)
        return bytes
    }

    @Test
    fun testExoDecoder() {
        val resp: Response = Net.get("https://download.samplelib.com/mp3/sample-3s.mp3").execute()
        assert(resp.isSuccessful)

        val bytes = resp.body!!.byteStream()


        val audioPlayer = AudioUtils.createAudioTrack(44100)
        audioPlayer.play()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            val decoder = ExoAudioDecoder(context)
            decoder.callback = object : ExoAudioDecoder.Callback {
                override fun onReadPcmAudio(byteBuffer: ByteBuffer) {
                    println(byteBuffer)

                    val bufferSize =
                        audioPlayer.bufferSizeInFrames * audioPlayer.channelCount * 2 // Assuming 16-bit PCM
                    val tempBuffer = ByteArray(bufferSize)
//                    byteBuffer.rewind() // Ensure ByteBuffer is at the beginning

                    while (byteBuffer.hasRemaining()) {
                        val bytesToRead = minOf(byteBuffer.remaining(), tempBuffer.size)
                        byteBuffer.get(tempBuffer, 0, bytesToRead)
                        audioPlayer.write(tempBuffer, 0, bytesToRead)
                    }
                }
            }

            try {
                decoder.doDecode(bytes)
            } catch (e: ExoPlaybackException) {
                e.printStackTrace()
            }
        }
    }
}