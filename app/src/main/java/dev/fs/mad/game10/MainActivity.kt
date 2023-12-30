package dev.fs.mad.game10

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import dev.fs.mad.game10.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var hasUserConsent : Boolean = false
    var appSharedPref: SharedPreferences? = null
    lateinit var mAdView: AdView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(1024,1024)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appSharedPref = getSharedPreferences(GlobalCon.appCode, MODE_PRIVATE)

        (application as GlobalCon).checkUserConsent(this, this, true)
        hasUserConsent = (application as GlobalCon).getUserConsent()

        MobileAds.initialize(
            this
        ) { }

        mAdView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)


        val game : GlobalWV = binding.gameView

        if(hasUserConsent){
            Handler(this.mainLooper).postDelayed({
                Log.d("status", GlobalCon.status)
                Log.d("gameURL", GlobalCon.gameURL)
                if (GlobalCon.status == "success") {
                    binding.navBar.visibility = View.VISIBLE
                    binding.navBar.setOnItemSelectedListener { item ->
                        when (item.itemId) {
                            R.id.nav_game -> {
                                recreate()
                                true
                            }
                            R.id.nav_notify -> {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                true
                            }
                            R.id.nav_policy -> {
                                binding.gameView.loadUrl(GlobalCon.policyURL)
                                true
                            }
                            else -> false
                        }
                    }
                } else {
                    binding.navBar.visibility = View.GONE
                    binding.adView.visibility = View.GONE
                }

                game.visibility = View.VISIBLE
                game.loadUrl(GlobalCon.gameURL)
            }, 1200)
        }

    }


}