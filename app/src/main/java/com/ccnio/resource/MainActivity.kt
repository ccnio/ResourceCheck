package com.ccnio.resource

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ccnio.mylib.LibActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LibActivity.start(this)
    }
}