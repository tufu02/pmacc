package com.pg.pmacc

import android.content.Context
import android.webkit.JavascriptInterface
import java.io.File

/**
 * 网页(home.html) 与原生之间的桥。
 * 改界面只需要替换 assets/home.html，不用动 dex。
 */
class Bridge(private val ctx: Context) {

    private val pkg = "com.pokemod.app.public"

    private val cbDir: File by lazy {
        File(ctx.getExternalFilesDir(null), "callbacks").apply { mkdirs() }
    }
    private val accDir: File by lazy {
        File(ctx.getExternalFilesDir(null), "accounts").apply { mkdirs() }
    }

    private fun sh(cmd: String): String = try {
        val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.trim()
    } catch (e: Exception) {
        "ERR ${e.message}"
    }

    // ---------- 登录回调 ----------

    @JavascriptInterface
    fun callbacks(): String {
        val list = cbDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.name }
            ?: emptyList()
        return list.joinToString(",", "[", "]") { "\"${it.name.replace("\"", "\\\"")}\"" }
    }

    @JavascriptInterface
    fun sendCallback(name: String): String {
        val f = File(cbDir, name)
        if (!f.exists()) return "missing"
        val uri = f.readText().trim()
        if (uri.isEmpty()) return "empty"
        val safe = uri.replace("'", "'\\''")
        val r = sh("am start -a android.intent.action.VIEW -d '$safe' -p com.nianticlabs.pokemongo")
        return if (r.contains("Error")) "fail: $r" else "ok"
    }

    @JavascriptInterface
    fun delCallback(name: String): String = File(cbDir, name).delete().toString()

    // ---------- Pokemod 账号快照 ----------

    @JavascriptInterface
    fun accounts(): String {
        val list = accDir.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted()
            ?: emptyList()
        return list.joinToString(",", "[", "]") { "\"${it.replace("\"", "\\\"")}\"" }
    }

    @JavascriptInterface
    fun saveAccount(name: String): String {
        val n = name.trim()
        if (n.isEmpty()) return "name required"
        val dir = File(accDir, n.replace("/", "_")).apply { mkdirs() }
        val out = sh(
            "PREFS=/data/data/$pkg/shared_prefs; " +
                "cp -f \$PREFS/Hawk2.xml \$PREFS/com.pokemod.app.public_preferences.xml \$PREFS/prefs.xml \$PREFS/crypto.KEY_256.xml '${dir.absolutePath}/' 2>/dev/null; " +
                "rm -rf '${dir.absolutePath}/agents'; cp -r /data/data/$pkg/files/agents '${dir.absolutePath}/agents' 2>/dev/null; echo ok"
        )
        return out
    }

    @JavascriptInterface
    fun useAccount(name: String): String {
        val dir = File(accDir, name)
        if (!dir.isDirectory) return "no such account"
        val out = sh(
            "am force-stop $pkg; " +
                "U=\$(stat -c %u /data/data/$pkg); " +
                "for f in Hawk2.xml com.pokemod.app.public_preferences.xml prefs.xml crypto.KEY_256.xml; do " +
                "  [ -f '${dir.absolutePath}/'\$f ] && cp -f '${dir.absolutePath}/'\$f /data/data/$pkg/shared_prefs/\$f && " +
                "  chown \$U:\$U /data/data/$pkg/shared_prefs/\$f && chmod 660 /data/data/$pkg/shared_prefs/\$f && " +
                "  restorecon /data/data/$pkg/shared_prefs/\$f 2>/dev/null; " +
                "done; " +
                "if [ -d '${dir.absolutePath}/agents' ]; then rm -rf /data/data/$pkg/files/agents; cp -r '${dir.absolutePath}/agents' /data/data/$pkg/files/agents; chown -R \$U:\$U /data/data/$pkg/files/agents; restorecon -R /data/data/$pkg/files/agents 2>/dev/null; fi; " +
                "am start -n $pkg/com.pokemod.app.activities.MainActivity >/dev/null 2>&1; echo ok"
        )
        return out
    }

    @JavascriptInterface
    fun delAccount(name: String): String = File(accDir, name).deleteRecursively().toString()

    @JavascriptInterface
    fun activeAccount(): String {
        val r = sh("grep -o 'name=\"activeAccount\">[^<]*' /data/data/$pkg/shared_prefs/com.pokemod.app.public_preferences.xml | sed 's/.*>//'")
        return r.trim()
    }
}
