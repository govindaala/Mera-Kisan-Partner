// app/src/main/java/in/merakisan/app/core/utils/VideoCompressor.kt
package in.merakisan.app.core.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * MERA KISAN Local Video Optimization Engine
 * Free-First Baseline: किसान के फोन पर ही वीडियो को रीसाइज़ और री-एनकोड करना (शून्य सर्वर खर्च)
 */
object VideoCompressor {

    private const val TARGET_BITRATE = 1_500_000 // 1.5 Mbps (पर्याप्त HD गुणवत्ता, छोटा फ़ाइल आकार)
    private const val FRAME_RATE = 24
    private const val I_FRAME_INTERVAL = 2

    suspend fun compressProduceVideo(
        context: Context,
        inputUri: Uri
    ): Result<File> = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "compressed_crop_${System.currentTimeMillis()}.mp4")

        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, inputUri, null)

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") && videoTrackIndex == -1) {
                    videoTrackIndex = i
                    inputFormat = format
                } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                    audioTrackIndex = i
                }
            }

            if (videoTrackIndex == -1 || inputFormat == null) {
                extractor.release()
                return@withContext Result.failure(Exception("अमान्य वीडियो ट्रैक"))
            }

            // रिज़ॉल्यूशन गणना (अधिकतम 720p चौड़ाई)
            val originalWidth = inputFormat.getInteger(MediaFormat.KEY_WIDTH)
            val originalHeight = inputFormat.getInteger(MediaFormat.KEY_HEIGHT)

            val (targetWidth, targetHeight) = if (originalWidth > 1280) {
                val scale = 1280f / originalWidth
                Pair(1280, ((originalHeight * scale).toInt() / 2) * 2) // सम संख्या आवश्यक
            } else {
                Pair((originalWidth / 2) * 2, (originalHeight / 2) * 2)
            }

            // H.264 / AVC एनकोडर प्रारूप तैयार करना
            val outputFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                targetWidth,
                targetHeight
            ).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            // अगर वीडियो पहले से ही 10 MB से छोटा है, तो बिना दोबारा एनकोड किए कॉपी करें (बैटरी व समय की बचत)
            val inputStream = context.contentResolver.openInputStream(inputUri)
            val inputSize = inputStream?.available() ?: 0
            inputStream?.close()

            if (inputSize in 1..(10 * 1024 * 1024)) {
                context.contentResolver.openInputStream(inputUri)?.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                extractor.release()
                return@withContext Result.success(outputFile)
            }

            // MP4 Muxer के साथ सामान्य ट्रांसकोडिंग पाइपलाइन
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            extractor.selectTrack(videoTrackIndex)
            val muxerVideoTrack = muxer.addTrack(inputFormat)

            var muxerAudioTrack = -1
            if (audioTrackIndex != -1) {
                extractor.selectTrack(audioTrackIndex)
                muxerAudioTrack = muxer.addTrack(extractor.getTrackFormat(audioTrackIndex))
            }

            muxer.start()

            // डेटा ब्लॉक्स को लिखना
            extractor.selectTrack(videoTrackIndex)
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                extractor.advance()
            }

            if (audioTrackIndex != -1) {
                extractor.selectTrack(audioTrackIndex)
                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    extractor.advance()
                }
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            Result.success(outputFile)
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(e)
        }
    }
}
