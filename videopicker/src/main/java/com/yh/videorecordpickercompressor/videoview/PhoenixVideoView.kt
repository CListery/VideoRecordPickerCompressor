package com.yh.videorecordpickercompressor.videoview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.yh.videorecordpickercompressor.R
import com.yh.videorecordpickercompressor.dialog.PhoenixLoadingDialog

class PhoenixVideoView : RelativeLayout {

    private lateinit var mVideoView: InternalVideoView
    private lateinit var mSeekBarProgress: SeekBar
    private lateinit var mSmallPlayBtn: ImageView
    private lateinit var mCurrentProgressTxt: TextView
    private lateinit var mTotalProgressTxt: TextView
    private lateinit var mCenterPlayBtn: ImageView
    private lateinit var mConfirmBtn: TextView
    private lateinit var mFileName: TextView
    private lateinit var mCancelBtn: ImageView

    private lateinit var mBottomController: LinearLayout
    private lateinit var mRootContainer: FrameLayout
    private lateinit var mAudioManager: AudioManager
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private lateinit var mContext: Context
    private lateinit var mVideoLayout: View
    private lateinit var mActivity: Activity
    private var videoPos: Int = 0
    private var state = 0
    private var mVideoPath: String? = null
    private lateinit var loadingDialog: PhoenixLoadingDialog

    private lateinit var mControlListener: ControlListener

    private val volumeReceiver by lazy { VolumeReceiver() }

    constructor(context: Context) : super(context, null)

    @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        this.mContext = context
        setupView()
        setupData()
        setupListener()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            onResume()
        } else {
            onPause()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onDestroy()
    }

    fun register(activity: Activity) {
        this.mActivity = activity
    }

    fun setVideoPath(path: String) {
        this.mVideoPath = path
        if (path.startsWith("http") || path.startsWith("https")) {
            mVideoView.setVideoURI(Uri.parse(path))
        } else {
            mVideoView.setVideoPath(mVideoPath)
        }
    }

    fun isPlaying(): Boolean {
        return mVideoView.isPlaying
    }

    fun onPause() {
        videoPos = mVideoView.currentPosition
        mVideoView.stopPlayback()
        mHandler.removeMessages(UPDATE_PROGRESS)
        mSmallPlayBtn.setImageResource(R.drawable.video_play_center)
    }

    fun onResume() {
        mVideoView.seekTo(videoPos)
        mVideoView.resume()
    }

    fun onDestroy() {
        mVideoView.stopPlayback()
        mContext.unregisterReceiver(volumeReceiver)
    }

    fun seekTo(position: Int) {
        mVideoView.seekTo(position)
    }

    private fun setupView() {
        mVideoLayout = LayoutInflater.from(mContext).inflate(R.layout.view_phoenix_video, this, true)
        mVideoView = mVideoLayout.findViewById(R.id.video_view) as InternalVideoView
        mSeekBarProgress = mVideoLayout.findViewById(R.id.seekbar_progress) as SeekBar
        mSmallPlayBtn = mVideoLayout.findViewById(R.id.iv_play) as ImageView
        mCurrentProgressTxt = mVideoLayout.findViewById(R.id.tv_currentProgress) as TextView
        mTotalProgressTxt = mVideoLayout.findViewById(R.id.tv_totalProgress) as TextView
        mBottomController = mVideoLayout.findViewById(R.id.ll_controller) as LinearLayout
        mRootContainer = mVideoLayout.findViewById(R.id.rl_container) as FrameLayout
        mCenterPlayBtn = mVideoLayout.findViewById(R.id.iv_center_play) as ImageView
        mConfirmBtn = mVideoLayout.findViewById(R.id.bt_confirm) as TextView
        mCancelBtn = mVideoLayout.findViewById(R.id.bt_cancel) as ImageView
        mFileName = mVideoLayout.findViewById(R.id.tv_file) as TextView
        mSeekBarProgress.isEnabled = false
        mSmallPlayBtn.isEnabled = false
        mCenterPlayBtn.isEnabled = false
        mConfirmBtn.isEnabled = false
        mCancelBtn.isEnabled = false
    }

    private fun setupData() {
        screenWidth = getScreenWidth(mContext)
        screenHeight = getScreenHeight(mContext)
        mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        mContext.registerReceiver(volumeReceiver, filter)
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            mSeekBarProgress.isEnabled = true
            mSmallPlayBtn.isEnabled = true
            mCenterPlayBtn.isEnabled = true
            mConfirmBtn.isEnabled = true
            mCancelBtn.isEnabled = true
        }, 1000)
    }

    private fun setupListener() {

        mConfirmBtn.setOnClickListener {
            mControlListener.onConfirm()
        }

        mCancelBtn.setOnClickListener {
            mControlListener.onCancel()
        }

        mCenterPlayBtn.setOnClickListener {
            mVideoView.start()
            mHandler.sendEmptyMessage(UPDATE_PROGRESS)
            mCenterPlayBtn.visibility = View.GONE
            mBottomController.visibility = View.VISIBLE
            mSmallPlayBtn.setImageResource(R.drawable.video_video_pause)
        }

        mSmallPlayBtn.setOnClickListener {
            if (mVideoView.isPlaying) {
                mSmallPlayBtn.setImageResource(R.drawable.video_play_center)
                mVideoView.pause()
                mHandler.removeMessages(UPDATE_PROGRESS)
                mCenterPlayBtn.visibility = View.VISIBLE
            } else {
                mSmallPlayBtn.setImageResource(R.drawable.video_video_pause)
                mVideoView.start()
                mHandler.sendEmptyMessage(UPDATE_PROGRESS)
                if (state == 0) state = 1
                mCenterPlayBtn.visibility = View.GONE
            }
        }

        mVideoView.setOnPreparedListener {
            it.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        }

        mVideoView.setOnCompletionListener {
            mSmallPlayBtn.setImageResource(R.drawable.video_play_center)
            mCenterPlayBtn.visibility = View.VISIBLE
            mBottomController.visibility = View.GONE
        }

        mVideoView.setStateListener(object : InternalVideoView.StateListener {

            override fun changeVolumn(detlaY: Float) {
                val maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val index = (detlaY / screenHeight * maxVolume.toFloat() * 3f).toInt()
                val volume = Math.max(0, currentVolume - index)
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }

            override fun changeBrightness(detlaX: Float) {
                val wml = mActivity.window.attributes
                var screenBrightness = wml.screenBrightness
                val index = detlaX / screenWidth.toFloat() / 3f
                screenBrightness += index
                if (screenBrightness > 1.0f) {
                    screenBrightness = 1.0f
                } else if (screenBrightness < 0.01f) {
                    screenBrightness = 0.01f
                }
                wml.screenBrightness = screenBrightness
                mActivity.window.attributes = wml
            }

            override fun hideHint() {

            }
        })

        mSeekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateTextViewFormat(mCurrentProgressTxt, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 暂停刷新
                mHandler.removeMessages(UPDATE_PROGRESS)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (state != 0) {
                    mHandler.sendEmptyMessage(UPDATE_PROGRESS)
                }
                mVideoView.seekTo(seekBar.progress)
            }
        })
    }

    /**
     * 屏幕状态改变

     * @param newConfig newConfig
     */
    public override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    /**
     * 格式化时间进度
     */
    private fun updateTextViewFormat(tv: TextView, m: Int) {

        val result: String
        // 毫秒转成秒
        val second = m / 1000
        val hour = second / 3600
        val minute = second % 3600 / 60
        val ss = second % 60

        if (hour != 0) {
            result = String.format("%02d:%02d:%02d", hour, minute, ss)
        } else {
            result = String.format("%02d:%02d", minute, ss)
        }
        tv.text = result
    }

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == UPDATE_PROGRESS) {

                // 获取当前时间
                val currentTime = mVideoView.currentPosition
                // 获取总时间
                val totalTime = mVideoView.duration - 100
                if (currentTime >= totalTime) {
                    mVideoView.pause()
                    mVideoView.seekTo(0)
                    mSeekBarProgress.progress = 0
                    mSmallPlayBtn.setImageResource(R.drawable.video_play_center)
                    updateTextViewFormat(mCurrentProgressTxt, 0)
                    this.removeMessages(UPDATE_PROGRESS)
                } else {
                    mSeekBarProgress.max = totalTime
                    mSeekBarProgress.progress = currentTime
                    updateTextViewFormat(mCurrentProgressTxt, currentTime)
                    updateTextViewFormat(mTotalProgressTxt, totalTime)
                    this.sendEmptyMessageDelayed(UPDATE_PROGRESS, 100)
                }
            }
        }
    }

    /**
     * 设置播放进度条样式

     * @param drawable drawable
     */
    fun setProgressBg(drawable: Drawable) {
        mSeekBarProgress.progressDrawable = drawable
    }

    fun getScreenWidth(context: Context): Int {

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.widthPixels
    }

    fun getScreenHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.heightPixels
    }

    fun dipToPx(context: Context, dipValue: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue.toFloat(), context
                .resources.displayMetrics).toInt()
    }

    /**
     * show loading loadingDialog
     */
    protected fun showLoadingDialog() {
        dismissLoadingDialog()
        loadingDialog = PhoenixLoadingDialog(context)
        loadingDialog.show()
    }

    /**
     * dismiss loading loadingDialog
     */
    protected fun dismissLoadingDialog() {
        try {
            if (loadingDialog != null && loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    internal inner class VolumeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //如果音量发生变化则更改seekbar的位置
            if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                // 当前的媒体音量
                val currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
        }
    }

    fun setControlListener(controlListener: ControlListener) {
        mControlListener = controlListener
    }

    interface ControlListener {
        fun onCancel()
        fun onConfirm()
    }

    companion object {
        private val UPDATE_PROGRESS = 1
    }
}
