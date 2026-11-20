package com.pg.pmacc

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private val pkg = "com.pokemod.app.public"
    private val dataDir = "/data/data/$pkg"
    private val prefsDir = "$dataDir/shared_prefs"
    private val prefFiles = listOf(
        "Hawk2.xml",
        "com.pokemod.app.public_preferences.xml",
        "prefs.xml",
        "crypto.KEY_256.xml"
    )

    private lateinit var listLayout: LinearLayout
    private lateinit var status: TextView
    private lateinit var accDir: File
    private lateinit var cbDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listLayout = findViewById(R.id.list)
        status = findViewById(R.id.status)
        accDir = File(getExternalFilesDir(null), "accounts")
        if (!accDir.exists()) accDir.mkdirs()
        cbDir = File(getExternalFilesDir(null), "callbacks")
        if (!cbDir.exists()) cbDir.mkdirs()

        findViewById<Button>(R.id.save).setOnClickListener { onSave() }
        findViewById<Button>(R.id.refresh).setOnClickListener { render() }

        checkRoot()
        render()
    }

    // ---------- root helpers ----------

    private fun exec(bin: String, cmd: String): String {
        return try {
            val process = ProcessBuilder(bin, "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val out = process.inputStream.bufferedReader().readText()
            process.waitFor()
            out
        } catch (e: Exception) {
            "ERR: ${e.message}"
        }
    }

    private fun sh(cmd: String): String {
        val first = exec("su", cmd)
        if (!first.startsWith("ERR")) return first
        val second = exec("/debug_ramdisk/su", cmd)
        if (!second.startsWith("ERR")) return second
        return first
    }

    private fun checkRoot() {
        val id = sh("id").trim()
        status.text = if (id.contains("uid=0")) "root 已就绪" else "未获得 root：$id"
        status.setTextColor(if (id.contains("uid=0")) Color.parseColor("#2E7D32") else Color.parseColor("#C62828"))
    }

    // ---------- save ----------

    private fun currentAccountName(): String {
        val raw = sh("grep -o 'name=\"activeAccount\">[^<]*' $prefsDir/com.pokemod.app.public_preferences.xml")
        val name = raw.substringAfterLast('>').trim()
        return name.ifBlank { "acc-" + System.currentTimeMillis() }
    }

    private fun onSave() {
        val default = currentAccountName()
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(default)
            setSelection(default.length)
        }
        AlertDialog.Builder(this)
            .setTitle("保存当前账号")
            .setMessage("给这份快照起个名字")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) saveSnapshot(name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveSnapshot(name: String) {
        val safe = name.replace("/", "_")
        val dir = "${accDir.absolutePath}/$safe"
        val copies = prefFiles.joinToString(" ") { f ->
            "[ -f $prefsDir/$f ] && cp -f $prefsDir/$f '$dir/'"
        }
        val cmd = """
            mkdir -p '$dir'
            $copies
            rm -rf '$dir/agents'
            [ -d $dataDir/files/agents ] && cp -r $dataDir/files/agents '$dir/agents'
            echo done
        """.trimIndent()
        val out = sh(cmd)
        Toast.makeText(this, if (out.contains("done")) "已保存：$safe" else "保存失败：$out", Toast.LENGTH_LONG).show()
        render()
    }

    // ---------- switch ----------

    private fun useSnapshot(name: String) {
        val dir = "${accDir.absolutePath}/$name"
        val restores = prefFiles.joinToString("\n") { f ->
            "[ -f '$dir/$f' ] && cp -f '$dir/$f' $prefsDir/$f && chown ${'$'}U:${'$'}U $prefsDir/$f && chmod 660 $prefsDir/$f && restorecon $prefsDir/$f 2>/dev/null"
        }
        val cmd = """
            am force-stop $pkg
            U=${'$'}(stat -c %u $dataDir)
            $restores
            if [ -d '$dir/agents' ]; then
              rm -rf $dataDir/files/agents
              cp -r '$dir/agents' $dataDir/files/agents
              chown -R ${'$'}U:${'$'}U $dataDir/files/agents
              restorecon -R $dataDir/files/agents 2>/dev/null
            fi
            am start -n $pkg/com.pokemod.app.activities.MainActivity >/dev/null 2>&1
            echo switched
        """.trimIndent()
        val out = sh(cmd)
        Toast.makeText(this, if (out.contains("switched")) "已切换到：$name" else "切换失败：$out", Toast.LENGTH_LONG).show()
    }

    private fun deleteSnapshot(name: String) {
        AlertDialog.Builder(this)
            .setTitle("删除快照")
            .setMessage("确定删除 $name ？")
            .setPositiveButton("删除") { _, _ ->
                sh("rm -rf '${accDir.absolutePath}/$name'")
                render()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------- list ----------

    private fun renderCallbacks(): Boolean {
        val cbs = cbDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.name } ?: emptyList()
        if (cbs.isEmpty()) return false
        listLayout.addView(TextView(this).apply {
            text = "登录回调（选一个发送给 Pokémon GO 即用它登录）"
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 8, 0, 4)
        })
        for (f in cbs) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }
            val label = TextView(this).apply {
                text = f.name
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnLogin = Button(this).apply {
                text = "登录到 PoGo"
                setOnClickListener { loginToPogo(f) }
            }
            val btnDel = Button(this).apply {
                text = "删除"
                setOnClickListener { f.delete(); render() }
            }
            row.addView(label); row.addView(btnLogin); row.addView(btnDel)
            listLayout.addView(row)
        }
        listLayout.addView(TextView(this).apply {
            text = "账号快照"
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 20, 0, 4)
        })
        return true
    }

    private fun loginToPogo(f: File) {
        val uri = f.readText().trim()
        if (uri.isEmpty()) return
        val safe = uri.replace("'", "'\\''")
        val out = sh("am start -a android.intent.action.VIEW -d '$safe' -p com.nianticlabs.pokemongo")
        Toast.makeText(this, if (out.contains("Error")) "失败：$out" else "已发送给 Pokémon GO", Toast.LENGTH_LONG).show()
    }

    private fun render() {
        listLayout.removeAllViews()
        renderCallbacks()
        val items = accDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: emptyList()
        if (items.isEmpty()) {
            listLayout.addView(TextView(this).apply {
                text = "还没有保存的账号。先在 Pokemod 里登录账号，再点上面的「保存当前账号」。"
                setPadding(0, 24, 0, 24)
            })
            return
        }
        for (dir in items) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 12, 0, 12)
            }
            val label = TextView(this).apply {
                text = dir.name
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnUse = Button(this).apply {
                text = "切换"
                setOnClickListener { useSnapshot(dir.name) }
            }
            val btnDel = Button(this).apply {
                text = "删除"
                setOnClickListener { deleteSnapshot(dir.name) }
            }
            row.addView(label)
            row.addView(btnUse)
            row.addView(btnDel)
            listLayout.addView(row)
        }
    }
}
