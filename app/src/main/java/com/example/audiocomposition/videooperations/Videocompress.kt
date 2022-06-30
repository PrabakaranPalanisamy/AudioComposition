import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.ait.qliq.ui.fragment.FFmpegCallBack
import com.example.audiocomposition.videooperations.Switchfile
import com.example.audiovideomerge.Common
import com.example.audiovideomerge.Common.getFilePath
import com.example.audiovideomerge.videooperations.CallBackOfQuery
import com.simform.videooperations.LogMessage
import java.io.File

class Videocompress() {
    private var isInputVideoSelected: Boolean = false

    companion object {
        val ffmpegQueryExtension = FFmpegQueryExtension()
        var height: Int? = 720
        var width: Int? = 1280

        fun compressProcess(
            input: String,
            mcontext: Activity,
            switchfile: Switchfile
        ): String {
            val outputPath = getFilePath(mcontext, Common.VIDEO)
            var result = ""
            val query = ffmpegQueryExtension.compressor(input, width, height, outputPath)
            CallBackOfQuery().callQuery(query, object : FFmpegCallBack {
                override fun process(logMessage: LogMessage) {
                    Log.e("videocompresscalling", "processing");
                }

                @SuppressLint("SetTextI18n")
                override fun success() {
                    val ouput: String =
                        String.format(outputPath, Common.getFileSize(File(outputPath)))
                    result = ouput
                    switchfile.getFile(result)
                }

                override fun cancel() {
                    Log.e("videocompresscalling", "cancel")
                }

                override fun failed() {
                    result = ""
                    Log.e("videocompresscalling", "failure");
                }
            })
            return result
        }

        fun mergeAudioVideo(
            inputAudio: String,
            inputVideo: String,
            offset:String,
            duration:String,
            videoOffset:String,videoDuration:String,
            mcontext: Activity,
            switchfile: Switchfile
        ): String {

            val outputPath = getFilePath(mcontext, Common.VIDEO)
            var result = ""
            Log.e("videocompress ",inputAudio)
            Log.e("videocompress ",inputVideo)
            Log.e("videocompress ",outputPath)
            val query = ffmpegQueryExtension.mergeAudioVideo3(inputVideo,inputAudio , outputPath,offset,duration,videoOffset,videoDuration,"4")
//            val query = ffmpegQueryExtension.mergeAudioVideo(inputVideo,inputAudio , outputPath)

            CallBackOfQuery().callQuery(query, object : FFmpegCallBack {
                override fun process(logMessage: LogMessage) {
                    Log.e("videocompresscalling", "processing");
                }

                @SuppressLint("SetTextI18n")
                override fun success() {
                    val ouput: String =
                        String.format(outputPath, Common.getFileSize(File(outputPath)))
                    result = ouput
                    switchfile.getFile(result)
                }

                override fun cancel() {
                    Log.e("videocompresscalling", "cancel")
                }

                override fun failed() {
                    result = ""
                    Log.e("videocompresscalling", "failure");
                }
            })
            return result
        }
    }



}