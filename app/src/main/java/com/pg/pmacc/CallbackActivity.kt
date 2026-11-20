package com.pg.pmacc

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallbackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (uri == null) {
            Toast.makeText(this, "没有收到回调数据", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val dir = File(getExternalFilesDir(null), "callbacks").apply { mkdirs() }
        val stamp = SimpleDateFormat("MMdd-HHmmss", Locale.US).format(Date())
        val f = File(dir, "callback-$stamp.txt")
        f.writeText(uri.toString())
        Toast.makeText(this, "已保存回调：${f.name}", Toast.LENGTH_LONG).show()
        finish()
    }
}
