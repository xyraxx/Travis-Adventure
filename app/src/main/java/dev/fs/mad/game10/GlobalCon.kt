package dev.fs.mad.game10

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.PusherEvent
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.NoSuchAlgorithmException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class GlobalCon : Application(){

    private lateinit var urlCrypt: CryptURL
    private var hasUserConsent = false

    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences(appCode, MODE_PRIVATE)
        hasUserConsent = prefs.getBoolean(userConsent, false)

        val options = PusherOptions()
        options.setCluster("ap1")

        val pusher = Pusher("fdcb398aff6445bc7bd6", options)

        pusher.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                Log.i(
                    "Pusher", "State changed from " + change.previousState +
                            " to " + change.currentState
                )
            }

            override fun onError(message: String, code: String, e: java.lang.Exception) {
                var errorCode = code

                if (errorCode == null) {
                    Log.e("Pusher", "Received null code in onError method.")
                    // Handle the null code appropriately or assign a default value.
                    errorCode = "UNKNOWN_ERROR_CODE"
                }

                Log.i(
                    "Pusher", """There was a problem connecting! code: $errorCode message: $message Exception: $e """.trimIndent()
                )
            }
        }, ConnectionState.ALL)

        val channel = pusher.subscribe(packageName)

        channel.bind("my-event") { event: PusherEvent ->
            try {
                val notifyMsg = JSONObject(event.data)
                val notificationManager =
                    getSystemService(
                        NotificationManager::class.java
                    )
                if (!notificationManager.areNotificationsEnabled()) {
                    // Notifications are disabled, guide the user to enable them
                    // You can also open the app settings page for notifications
                    val intent =
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } else {
                    showNotification(
                        "Announcement",
                        notifyMsg.getString("message"),
                        notifyMsg.getString("url")
                    )
                }
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
    }


    private fun showNotification(title: String, message: String, link: String) {
        val remoteViews = RemoteViews(packageName, R.layout.notification_ui)
        remoteViews.setTextViewText(R.id.notificationTitle, title)
        remoteViews.setTextViewText(R.id.notificationMessage, message)

        // Create an intent to open the link when the button is clicked
        val openLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openLinkIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.openLinkButton, pendingIntent)
        val channel = NotificationChannel(
            "my-channel",
            "Announcements",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "my-channel")
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setAutoCancel(true)
        val notificationMg = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationMg.notify(1, builder.build())
    }

    class CryptURL {
        companion object {
            private lateinit var keySpec: SecretKeySpec
            private lateinit var cipher: Cipher
            private const val key = "21913618CE86B5D53C7B84A75B3774CD"
            private const val transformation = "AES/CBC/NoPadding"

            @Throws(Exception::class)
            fun decrypt(encryptedData: String?, secretKey: String): String {
                val encryptedBytes = Base64.getDecoder().decode(encryptedData)
                val iv = ByteArray(16)
                System.arraycopy(encryptedBytes, 0, iv, 0, 16)
                val ciphertext = ByteArray(encryptedBytes.size - 16)
                System.arraycopy(encryptedBytes, 16, ciphertext, 0, ciphertext.size)
                val secretKeySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), "AES")
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(iv))
                val decryptedBytes = cipher.doFinal(ciphertext)
                return String(decryptedBytes, StandardCharsets.UTF_8).trim { it <= ' ' }
            }
        }

        init {
            keySpec =
                SecretKeySpec("21913618CE86B5D53C7B84A75B3774CD".toByteArray(), "AES")
            try {
                cipher =
                    Cipher.getInstance("AES/CBC/NoPadding")
            } catch (var2: NoSuchPaddingException) {
                var2.printStackTrace()
            } catch (var2: NoSuchAlgorithmException) {
                var2.printStackTrace()
            }
        }


    }

    fun setupConfig(context:Context, activity:Activity, hasPolicy: Boolean) {
        urlCrypt = CryptURL()
        ApiHelper.init(this)
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val requestBody = JSONObject()
        try {
            requestBody.put("appid", appCode)
            requestBody.put("package", packageName)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val endPoint = "$apiURL?appid=$appCode&package=${packageName}"
        Log.d("urlResult", endPoint)

        val myReq = JsonObjectRequest(0, endPoint, requestBody,
            { response: JSONObject ->
                Log.e("urlResponse", "JSON:Response - $response")

                try {
                    val decryptedText = CryptURL.decrypt(response.getString("data"), "21913618CE86B5D53C7B84A75B3774CD")
                    val jsonData = JSONObject(decryptedText)
                    gameURL = jsonData.getString("gameURL")
                    status = jsonData.getString("status")
                    Log.d("status in json" , status)
                    Log.e("decrypURL", "Decrypted: $decryptedText")

                } catch (var6: Exception) {
                    var6.printStackTrace()
                }
            },
            { error ->
                Log.e("VolleyError", "Error: ${error.message}")
            })

        requestQueue.add(myReq)
    }

    companion object {
        const val appCode = "7T"
        var apiURL = "https://backend.madgamingdev.com/api/gameid"
        var gameURL = ""
        var policyURL = "https://sites.google.com/view/777t/home"
        var jsInterface = "jsBridge"
        var status = ""
        var nav = false
        var userConsent = "userConsent"
    }

    fun checkUserConsent(context: Context, activity: Activity, hasPolicy: Boolean) {
        setupConfig(context, activity, hasPolicy)
        if (!hasUserConsent && hasPolicy) {
            showConsentDialog(context, activity)
        }
    }

    private fun showConsentDialog(context: Context, activity: Activity) {
        val builder = AlertDialog.Builder(context)
        val dialogView: View = LayoutInflater.from(context).inflate(R.layout.window_uc, null)
        val consent = dialogView.findViewById<WebView>(R.id.consentWindow)
        consent.webViewClient = WebViewClient()
        consent.loadUrl("https://sites.google.com/view/777t/home")
        builder.setTitle("Data Privacy Policy")
        builder.setView(dialogView)
        builder.setPositiveButton("I Agree") { dialog: DialogInterface?, which: Int ->
            setConsentValue(true)
            loadActivity(activity)
            consent.visibility = View.GONE

        }
        builder.setNegativeButton(
            "Don't Agree"
        ) { dialog: DialogInterface?, which: Int -> activity.finishAffinity() }
        builder.show()
    }

    private fun setConsentValue(userChoice: Boolean) {
        hasUserConsent = userChoice
        val prefs = getSharedPreferences(appCode, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(userConsent, userChoice)
        editor.apply()
    }

    fun getUserConsent(): Boolean {
        val prefs = getSharedPreferences(appCode, MODE_PRIVATE)
        hasUserConsent =  prefs.getBoolean(userConsent, false)
        return hasUserConsent
    }

    private fun loadActivity(activity: Activity) {
        val newActivity = Intent(activity, MainActivity::class.java)
        newActivity.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(newActivity)
    }
}