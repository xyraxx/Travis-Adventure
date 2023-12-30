package dev.fs.mad.game10

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class ApiHelper {
    companion object {
        private var mRequestQueue: RequestQueue? = null
        fun init(context: Context?) {
            mRequestQueue = Volley.newRequestQueue(context)
        }
    }
}