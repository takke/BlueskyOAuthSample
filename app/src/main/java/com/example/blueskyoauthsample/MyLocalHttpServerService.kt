//package com.example.blueskyoauthsample
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.IBinder
//import android.util.Log
//import androidx.core.app.NotificationCompat
//import io.ktor.application.ApplicationStarted
//import io.ktor.application.ApplicationStopping
//import io.ktor.application.call
//import io.ktor.response.respondText
//import io.ktor.routing.get
//import io.ktor.routing.routing
//import io.ktor.server.engine.embeddedServer
//import io.ktor.server.netty.Netty
//import io.ktor.server.netty.NettyApplicationEngine
//
//class MyLocalHttpServerService : Service() {
//
//    private lateinit var server: NettyApplicationEngine
//
//    override fun onBind(intent: Intent?): IBinder? {
//        // バインド不要のためnullを返す
//        return null
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//
//        Log.i(TAG, "onStartCommand: start")
//
//        // フォアグラウンドサービスとしての通知を作成
//        val notification = createNotification()
//        startForeground(1, notification)
//
//        Log.i(TAG, "onStartCommand: notification created")
//
//        // サーバーの起動
//        server = embeddedServer(Netty, host = "127.0.0.1", port = 8080) {
//
//            environment.monitor.subscribe(ApplicationStarted) {
//                // サーバーが起動したときの処理
//                Log.i(TAG, "Server has started successfully!")
//            }
//
//            environment.monitor.subscribe(ApplicationStopping) {
//                Log.i(TAG, "Server has stopping!")
//            }
//
//            routing {
//                get("/zonepane") {
//                    call.respondText("Hello, world!")
//                }
//
//                get("/") {
//                    call.respondText("Hello, world from root!")
//                }
//            }
//        }
//        server.start(wait = false)
//
//        Log.i(TAG, "onStartCommand: server started")
//
//        return START_STICKY // サービスが自動的に再起動されるようにする
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//
//        // サーバーの停止
//        server.stop(1000, 10000)
//
//        Log.i(TAG, "onDestroy: server stopped")
//    }
//
//    // 通知を作成するメソッド
//    private fun createNotification(): Notification {
//        val channelId = "server_channel"
//        val channelName = "Server Notification"
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        return NotificationCompat.Builder(this, channelId)
//            .setContentTitle("Server Running")
//            .setContentText("The Ktor server is running.")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .build()
//    }
//
//    companion object {
//        private const val TAG = "LocalServer"
//    }
//}