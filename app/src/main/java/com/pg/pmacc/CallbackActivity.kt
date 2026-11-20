package com.pg.pmacc

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 接收 PoGo 登录后的 deep link 回调（scheme: pokemongo）。
 * 在浏览器完成登录后，系统会弹出“用哪个应用打开”，选本应用即可把回调原样存下来。
 */
class CallbackActivity : AppCompatActivity() {

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
        val file = File(dir, "callback-$stamp.txt")
        file.writeText(uri.toString())
        Toast.makeText(this, "已保存登录回调：${file.name}", Toast.LENGTH_LONG).show()
        try {
            startActivity(
                android.content.Intent(this, MainActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) { }
        finish()
    }
}
