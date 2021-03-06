package com.example.youtubewidgetkotlin

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class FloatingWidgetService : Service() {

    private lateinit var floatingView: View
    private var layoutParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    internal lateinit var youTubePlayer: YouTubePlayer

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        createNotification()
        createFloatingView()
        createLayoutParams()
        createWindowManager()
        addFloatingViewToWindowManager()

        getYouTubePlayer()

        setViewItemsListeners()

    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createOwnNotificationChannel()
        } else {
            startForeground(1, Notification())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createOwnNotificationChannel() {
        val notificationChannelId: String = packageName
        val channelName = "YoutubeService"
        val channel =
            NotificationChannel(
                notificationChannelId, channelName,
                NotificationManager.IMPORTANCE_NONE
            )

        channel.lightColor = Color.RED
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Youtube service is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    private fun createFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val layoutFlag: Int = createLayoutFlag()
        //Create view params
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag, 0, //WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams?.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE// | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        //Specify the view position
        //Initially view will be added to top-left corner
        layoutParams?.gravity = Gravity.TOP or Gravity.START
        layoutParams?.x = 0
        layoutParams?.y = 100
        return layoutParams as WindowManager.LayoutParams
    }

    private fun createLayoutFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun createWindowManager() {
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager?
    }

    private fun addFloatingViewToWindowManager() {
        mWindowManager?.addView(floatingView, layoutParams)
    }

    private fun getYouTubePlayer() {
        val youTubePlayerView: YouTubePlayerView = floatingView.findViewById(R.id.ytPlayerView)
        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@FloatingWidgetService.youTubePlayer = youTubePlayer
            }
        })
    }

    private fun setViewItemsListeners() {
        setFloatingViewOnTouchListener()
        setSearchBarOnClickListener()
        setButtonSearchOnClickListener()
        setButtonResizeOnTouchListener()
        setButtonMoveOnTouchListener()
    }

    private fun setFloatingViewOnTouchListener() {
        //Clicking outside widget invokes floatingView touch event
        //that is used for getting focus off widget to allow using phone
        floatingView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onFloatingViewDownTouch()
            }
            false
        }
    }

    private fun onFloatingViewDownTouch() {
        val etSearch: EditText = floatingView.findViewById(R.id.etSearch)
        if (etSearch.hasFocus()) {
            layoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            layoutParams?.flags = 0
        }
        mWindowManager?.updateViewLayout(floatingView, layoutParams)
    }

    private fun setSearchBarOnClickListener() {
        val etSearch: EditText = floatingView.findViewById(R.id.etSearch)
        etSearch.setOnClickListener {
            //Make widget focusable by clearing flags
            layoutParams?.flags = 0
            mWindowManager?.updateViewLayout(floatingView, layoutParams)
        }
    }

    private fun setButtonSearchOnClickListener() {
        val imgBtnSearch: ImageButton = floatingView.findViewById(R.id.imgBtnSearch)
        imgBtnSearch.setOnClickListener {
            val etSearch: EditText = floatingView.findViewById(R.id.etSearch)
            etSearch.clearFocus()
            layoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            mWindowManager?.updateViewLayout(floatingView, layoutParams)

            val searchQuery: String = etSearch.text.toString()
            performVideoSearchAndPlay(searchQuery)
        }
    }

    private fun performVideoSearchAndPlay(searchQuery: String) {
        Thread {
            val videoInformationObject = VideosInformation(searchQuery, applicationContext).getFirstVideoInfo()
            val videoId: String = videoInformationObject?.getJSONObject("id")?.getString("videoId") ?: ""
            youTubePlayer.loadVideo(videoId, 0f)
        }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setButtonResizeOnTouchListener() {
        val imgBtnResize: ImageButton = floatingView.findViewById(R.id.imgBtnResize)

        imgBtnResize.setOnTouchListener(object : View.OnTouchListener {
            var initialTouchX: Float = 0.0f
            var initialTouchY: Float = 0.0f
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {

                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val xDiff: Int = (event.rawX - initialTouchX).toInt()
                        val yDiff: Int = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(xDiff) > 9 || Math.abs(yDiff) > 9) {
                            layoutParams?.width = floatingView.width + xDiff
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            mWindowManager?.updateViewLayout(floatingView, layoutParams)
                        }
                    }
                }
                return false
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setButtonMoveOnTouchListener() {
        val btnMove: Button = floatingView.findViewById(R.id.btnMove)

        btnMove.setOnTouchListener(object : View.OnTouchListener{

            var initialX: Int? = 0
            var initialY: Int? = 0
            var initialTouchX: Float = 0.0f
            var initialTouchY: Float = 0.0f
            var deltaTime: Double = 0.0
            var startTime: Double = 0.0
            var moved: Boolean = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        moved = false

                        //Remember the initial position
                        initialX = layoutParams?.x
                        initialY = layoutParams?.y

                        startTime = System.currentTimeMillis().toDouble()

                        //Get the touch location
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val xDiff: Int = (event.rawX - initialTouchX).toInt()
                        val yDiff: Int = (event.rawY - initialTouchY).toInt()

                        layoutParams?.x = initialX?.plus(xDiff)
                        layoutParams?.y = initialY?.plus(yDiff)
                        mWindowManager?.updateViewLayout(floatingView, layoutParams)

                        if (xDiff > 10 || yDiff > 10) {
                            moved = true
                        }
                    }
                }

                deltaTime = System.currentTimeMillis() - startTime
                if(deltaTime > 1000 && !moved) {
                    stopSelf()
                }
                return false
            }

        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        mWindowManager?.removeView(floatingView)
    }
}