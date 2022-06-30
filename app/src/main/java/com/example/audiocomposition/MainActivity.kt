package com.example.audiocomposition

import Videocompress.Companion.mergeAudioVideo
import android.Manifest
import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.audiocomposition.videooperations.Switchfile
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.pro.audiotrimmer.AudioTrimmerView
import java.io.*
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), AudioTrimmerView.OnSelectedRangeChangedListener {
    var chosenAudioFile: File? = null
    var chosenVideoFile: File? = null
    var audioUri: Uri? = null
    var videoUri: Uri? = null

    var imgPlayPauseAudio: ImageView? = null
    var imgExport: ImageView? = null
    var txtAudioDuration: TextView? = null

    //    var audioUri: Uri? = null
    var startTimeMillis: Long = 0;
    var endTimeMillis: Long = 0;

    //    var videoUri: Uri? = null
    var mediaPlayer: MediaPlayer? = null
    var runable: Runnable? = null
    var mHandler: Handler? = null
    var executorService: ExecutorService = Executors.newSingleThreadExecutor()
    var longRunningTaskFuture: Future<*>? = null
    var audiview: AudioTrimmerView? = null

    var exoPlayer: SimpleExoPlayer? = null
    var expVideoPlayer: PlayerView? = null
    override fun onDestroy() {

        runable?.let { mHandler?.removeCallbacks(it) }
        longRunningTaskFuture?.cancel(true)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        runable = null
        mHandler = null
        chosenAudioFile = null
        chosenVideoFile = null
        super.onDestroy()

    }

    override fun onPause() {
        super.onPause()
        imgPlayPauseAudio!!.setImageDrawable(
            baseContext.resources.getDrawable(
                R.drawable.ic_play
            )
        )
        mediaPlayer?.pause()
        exoPlayer?.pause()
    }

    private fun initializePlayer(vidioUri: Uri) {
        exoPlayer = SimpleExoPlayer.Builder(baseContext).build()
        expVideoPlayer!!.setPlayer(exoPlayer)
        expVideoPlayer!!.hideController()
        val mediaSource: MediaSource = buildMediaSource(vidioUri, baseContext)
        exoPlayer!!.setPlayWhenReady(false)
//        exoPlayer!!.seekTo(currentWindow, playbackPosition)
        exoPlayer!!.setMediaSource(mediaSource)
        exoPlayer!!.prepare()
        exoPlayer!!.volume = 0f

    }

    private fun buildMediaSource(uri: Uri, context: Context): MediaSource {
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(context, "exoplayer-codelab")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var btnchooseAudio = findViewById<Button>(R.id.btnchooseAudio);
        var btnchooseVideo = findViewById<Button>(R.id.btnchooseVideo);
        var btnMerge = findViewById<Button>(R.id.btnMerge);
        audiview = findViewById<AudioTrimmerView>(R.id.audioTrimmer);
        expVideoPlayer = findViewById<PlayerView>(R.id.expVideoPlayer);
        imgPlayPauseAudio = findViewById<ImageView>(R.id.imgPlayPauseAudio);
        txtAudioDuration = findViewById<TextView>(R.id.txtAudioDuration);
        imgExport = findViewById<ImageView>(R.id.imgExport);
        val onActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult> {
                override fun onActivityResult(result: ActivityResult) {
                    if (result.resultCode == RESULT_OK) {

                        val data = result.data
                        val uri = data!!.data
                        audioUri = uri
//                        setupAudioFile(audioUri!!)
                        btnchooseAudio.setText(uri!!.path)

                    }
                }

                private val contentResolver: ContentProvider?
                    private get() = null
            })
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            }
        val onActivityVideoResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult> {
                override fun onActivityResult(result: ActivityResult) {
                    if (result.resultCode == RESULT_OK) {

                        val data = result.data
                        val uri = data!!.data
                        videoUri = uri
                        btnchooseVideo.setText(uri!!.path)
                        setupVideoFile(videoUri!!)
                    }
                }

                private val contentResolver: ContentProvider?
                    private get() = null
            })

        btnchooseAudio.setOnClickListener {
            if (checkPermission()) {
                val intent = Intent()
                intent.action = Intent.ACTION_GET_CONTENT
                intent.type = "audio/*"
                onActivityResultLauncher.launch(intent)
            } else {
                requestPermissionLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }

        imgExport!!.setOnClickListener {
            try {
                var videoDuration =Math.abs( extractAudioLength(chosenVideoFile!!.path)/1000).toString()
                Log.e("imgExport ","videoDuration "+videoDuration)
                var videoOffset = "00:00:00"
                var duration = Math.abs((endTimeMillis-startTimeMillis)/1000).toString();
                if(duration>videoDuration)
                    duration=videoDuration
                Log.e("imgExport ","audioduraation "+duration)
                var offset =timeConversion(startTimeMillis).toString()
                Log.e("imgExport ","offset "+offset)
                val filePath =
                    mergeAudioVideo(
                        chosenAudioFile!!.getPath(),
                        chosenVideoFile!!.getPath(), offset,duration,videoOffset,videoDuration,
                        this,
                        object : Switchfile {
                            @Throws(URISyntaxException::class)
                            override fun getFile(file: String?) {
                                Log.e("filesize", "after::" + File(file).length() / 1024f)
                                runOnUiThread {
                                   Toast.makeText(this@MainActivity,"composition sucess ",Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
        btnMerge.setOnClickListener {

            if (audioUri != null && videoUri != null) {
//                var intentd = Intent(this,MergeActivity::class.java)
//                intentd.putExtra("audioUri",chosenAudioFile!!.absolutePath)
//                intentd.putExtra("videoUri",chosenVideoFile!!.absolutePath)
//                startActivity(intentd)
                if (chosenAudioFile == null) {
                    val ins: InputStream
                    val FILE_NAM = "mergevideo"
                    val directory: File = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                    val file = File(directory.toString())
                    val outputFile =
                        file.toString() + File.separator + FILE_NAM + "_audiotmp.mp3"
                    ins = baseContext.getContentResolver().openInputStream(audioUri!!)!!
                    chosenAudioFile = createFileFromInputStream(ins, outputFile)
                    audiview!!.setAudio(chosenAudioFile!!);
                    audiview!!.show()
                    audiview!!.setTotalAudioLength(extractAudioLength(chosenAudioFile!!.path))
                    audiview!!.setMaxDuration(extractAudioLength(chosenVideoFile!!.path));
                    audiview!!.setMinDuration(extractAudioLength(chosenVideoFile!!.path))
//                    audiview!!.setExtraDragSpace(80F)
//                    audiview!!.setMinDuration(10000)
                    getDummyWaveSample()
//                        audiview!!.setAudioSamples()
                    audiview!!.setOnSelectedRangeChangedListener(this@MainActivity)
                    showAudioPreview(true, chosenAudioFile!!)
                }
            }
//                mergeaudioVideo(audioUri!!, videoUri!!);
        }
        btnchooseVideo.setOnClickListener {
            if (checkPermission()) {
                val intent = Intent()
                intent.action = Intent.ACTION_GET_CONTENT
                intent.type = "video/mp4"
                onActivityVideoResultLauncher.launch(intent)
            } else {
                requestPermissionLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }

    }
    fun timeConversion(millie: Long?): String? {
        return if (millie != null) {
            val seconds = millie / 1000
            val sec = seconds % 60
            val min = seconds / 60 % 60
            val hrs = seconds / (60 * 60) % 24
            if (hrs > 0) {
                String.format("%02d:%02d:%02d", hrs, min, sec)
            } else {
                String.format("%02d:%02d", min, sec)
            }
        } else {
            null
        }
    }
    private fun checkPermission(): Boolean {

        return (ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)

    }

    fun setupAudioFile(audioUri: Uri) {
        val audioInputStream: InputStream
        val FILE_NAM = "audiovideomerge"
        val directory: File = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
        val audioFile = File(directory.toString())
        val outputAudioFilePath = audioFile.toString() + File.separator + FILE_NAM + "_audiotmp.mp3"
        audioInputStream = baseContext.getContentResolver().openInputStream(audioUri!!)!!
        chosenAudioFile = createFileFromInputStream(audioInputStream, outputAudioFilePath)
    }

    fun setupVideoFile(videoUrl: Uri) {
        val videoInputStream: InputStream
        val FILE_NAM = "audiovideomerge"
        val directory: File = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
        val videoFIle = File(directory.toString())
        val outputVideoFilePath = videoFIle.toString() + File.separator + FILE_NAM + "_videotmp.mp4"
        videoInputStream = baseContext.getContentResolver().openInputStream(videoUrl)!!
        chosenVideoFile = createFileFromInputStream(videoInputStream, outputVideoFilePath)
    }

    private fun createFileFromInputStream(inputStream: InputStream, fileName: String): File? {
        try {
            val f = File(fileName)
            f.setWritable(true, false)
            val outputStream: OutputStream = FileOutputStream(f)
            val buffer = ByteArray(1024)
            var length = 0
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.close()
            inputStream.close()
            return f
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getDummyWaveSample() {
        val data = ShortArray(50)
        for (i in data.indices)
            data[i] = Random().nextInt(data.size).toShort()
        audiview!!.setAudioSamples(data)
//        return data
    }

    fun showAudioPreview(show: Boolean, chosendFile: File) {

        if (show) {
            initializePlayer(videoUri!!)
            if (mediaPlayer == null)
                mediaPlayer = MediaPlayer()
            prepareMediaPlayer(chosendFile)
            if (mediaPlayer!!.isPlaying) {
                imgPlayPauseAudio!!.setImageDrawable(
                    baseContext.resources.getDrawable(
                        R.drawable.ic_pause
                    )
                )
            } else {
                imgPlayPauseAudio!!.setImageDrawable(
                    baseContext.resources.getDrawable(
                        R.drawable.ic_play
                    )
                )
            }
            mediaPlayer!!.setOnCompletionListener {
                imgPlayPauseAudio!!.setImageDrawable(
                    baseContext.resources.getDrawable(
                        R.drawable.ic_play
                    )
                )
            }
            if (mHandler == null)
                mHandler = Handler(Looper.getMainLooper())

            endTimeMillis=mediaPlayer!!.duration.toLong()

//           previewSeekbar.setOnSeekBarChangeListener(object :
//                SeekBar.OnSeekBarChangeListener {
//                override fun onStopTrackingTouch(seekBar: SeekBar) {}
//                override fun onStartTrackingTouch(seekBar: SeekBar) {}
//                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                    if (fromUser) {
//                       mediaPlayer?.seekTo(progress)
//                    }
//                }
//            })
            txtAudioDuration!!.setText(
                "0:00/" + calculateDuration(
                    mediaPlayer!!.getDuration().toLong()
                )
            )
            audiview!!.setAudioProgress(0)
            mediaPlayer!!.seekTo(0)
            imgPlayPauseAudio!!.setOnClickListener {

                if (!mediaPlayer!!.isPlaying) {
                    imgPlayPauseAudio!!.setImageDrawable(
                        baseContext.resources.getDrawable(
                            R.drawable.ic_pause
                        )
                    )
                    mediaPlayer!!.start()
                    exoPlayer!!.playWhenReady=true
                    runable = object : Runnable {
                        override fun run() {
                            // Updateing SeekBar every 100 miliseconds

                            mHandler?.postDelayed(this, 50)
                            audiview!!.setAudioProgress(
                                mediaPlayer?.getCurrentPosition()!!.toLong()
                            )
//                            Log.e("mediaplayer ","cutrrentpos "+mediaPlayer!!.currentPosition.toLong()+" endtimemilis "+endTimeMillis)
                            if (mediaPlayer!!.currentPosition.toLong() >= endTimeMillis) {
                                mediaPlayer!!.seekTo(startTimeMillis.toInt())
                                exoPlayer!!.seekTo(0,0)
                                return
                            }
                            //For Showing time of audio(inside runnable)
                            val miliSeconds: Long =
                                mediaPlayer?.getCurrentPosition()!!.toLong()
                            if (miliSeconds != 0L) {
                                //if audio is playing, showing current time;
                                val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(miliSeconds)
                                val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(miliSeconds)
                                if (minutes == 0L) {
                                    txtAudioDuration!!.setText(
                                        "0:" + seconds + "/" + calculateDuration(
                                            mediaPlayer?.getDuration()!!.toLong()
                                        )
                                    )
                                } else {
                                    if (seconds >= 60) {
                                        val sec = seconds - minutes * 60
                                        txtAudioDuration!!.setText(
                                            minutes.toString() + ":" + sec + "/" + calculateDuration(
                                                mediaPlayer?.getDuration()!!.toLong()
                                            )
                                        )
                                    }
                                }
                            } else {
                                //Displaying total time if audio not playing
                                val totalTime: Long = mediaPlayer?.getDuration()!!.toLong()
                                val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(totalTime)
                                val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(totalTime)
                                if (minutes == 0L) {
                                    txtAudioDuration!!.setText("0:$seconds")
                                } else {
                                    if (seconds >= 60) {
                                        val sec = seconds - minutes * 60
                                        txtAudioDuration!!.setText("$minutes:$sec")
                                    }
                                }
                            }
                        }
                    }
                    longRunningTaskFuture = executorService.submit(runable)
                    runable!!.run()
                } else {
                    mediaPlayer?.pause();
                    exoPlayer!!.playWhenReady=false
                    exoPlayer!!.pause()
                    imgPlayPauseAudio!!.setImageDrawable(
                        baseContext.resources.getDrawable(
                            R.drawable.ic_play
                        )
                    )
                }
            }
        }
    }


    private fun prepareMediaPlayer(
        chosendFile: File
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                mediaPlayer!!.setDataSource(
                    baseContext,
                    Uri.parse((chosendFile.absolutePath))
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                mediaPlayer!!.prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            try {
                mediaPlayer!!.setDataSource(
                    baseContext,
                    Uri.parse((chosendFile.absolutePath))
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                mediaPlayer!!.prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun calculateDuration(duration: Long): String? {
        var finalDuration = ""
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
        if (minutes == 0L) {
            finalDuration = "0:$seconds"
        } else {
            if (seconds >= 60) {
                val sec = seconds - minutes * 60
                finalDuration = "$minutes:$sec"
            }
        }
        return finalDuration
    }

    internal fun extractAudioLength(audioPath: String): Long {
        val retriever = try {
            MediaMetadataRetriever()
                .apply { setDataSource(audioPath) }
        } catch (e: IllegalArgumentException) {
            return 0L
        }

        val length = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()

        return length?.toLong() ?: 0L
    }


    override fun onSelectRangeStart() {
        Log.e("onSelectRangeStart", "");
//                                TODO("Not yet implemented")
    }

    override fun onSelectRange(startMillis: Long, endMillis: Long) {
        Log.e(
            "onSelectedRange",
            "statrt " + startMillis.toString() + " end " + endMillis.toString()
        )
//                                TODO("Not yet implemented")
    }

    override fun onSelectRangeEnd(startMillis: Long, endMillis: Long) {
        Log.e(
            "onSelectRangeEnd",
            "statrt " + startMillis.toString() + " end " + endMillis.toString()
        )
        startTimeMillis = startMillis;
        endTimeMillis = endMillis;
        mediaPlayer?.seekTo(startTimeMillis.toInt())
        exoPlayer!!.seekTo(0,0)
//                                TODO("Not yet implemented")
    }

    override fun onProgressStart() {
        Log.e("onProgressStart", "");
//                                TODO("Not yet implemented")
    }

    override fun onProgressEnd(millis: Long) {
        mediaPlayer?.seekTo(millis.toInt())
//        exoPlayer!!.seekTo(0,millis)
        Log.e("onProgressEnd", "statrt " + millis.toString())
//                                TODO("Not yet implemented")
    }

    override fun onDragProgressBar(millis: Long) {
        Log.e("onDragProgressBar", "statrt " + millis.toString())
//                                TODO("Not yet implemented")
    }
}