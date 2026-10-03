const fs = require('fs');

const filePath = 'd:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/browser/HomeFragment.kt';
let content = fs.readFileSync(filePath, 'utf8');

const oldCode = `    fun extractVideoDirectly(url: String) {
        val platformName = when {
            url.contains("facebook") || url.contains("fb.watch") -> "Facebook"
            url.contains("xnxx") -> "XNXX"
            url.contains("xvideos") -> "XVideos"
            url.contains("pornhub") -> "Pornhub"
            else -> com.nextaitechnology.antidetect.core.network.MonetizedDomainRegistry.extractHost(url).ifEmpty { "Video" }
        }
        Toast.makeText(context, "⚡ Đang bóc tách video " + platformName + "...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val videoInfo = snifferEngine.extractVideoFromUrl(url)
            if (videoInfo != null && videoInfo.qualities.isNotEmpty()) {
                activity?.runOnUiThread {
                    (activity as? MainActivity)?.notifyVideoInfoDetected(videoInfo)
                }
            } else {
                android.util.Log.w("NextAI_Sniffer", "Direct extraction returned null for: " + url)
            }
        }
    }`;

const newCode = `    fun extractVideoDirectly(url: String) {
        val platformName = when {
            url.contains("facebook") || url.contains("fb.watch") -> "Facebook"
            url.contains("xnxx") -> "XNXX"
            url.contains("xvideos") -> "XVideos"
            url.contains("pornhub") -> "Pornhub"
            else -> com.nextaitechnology.antidetect.core.network.MonetizedDomainRegistry.extractHost(url).ifEmpty { "Video" }
        }
        val safeContext = context ?: activity?.applicationContext
        if (safeContext != null) {
            Toast.makeText(safeContext, "⚡ Đang bóc tách video " + platformName + "...", Toast.LENGTH_SHORT).show()
        }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val videoInfo = snifferEngine.extractVideoFromUrl(url)
            if (videoInfo != null && videoInfo.qualities.isNotEmpty()) {
                activity?.runOnUiThread {
                    (activity as? MainActivity)?.notifyVideoInfoDetected(videoInfo)
                }
            } else {
                android.util.Log.w("NextAI_Sniffer", "Direct extraction returned null for: " + url)
            }
        }
    }`;

content = content.replace(oldCode, newCode);
fs.writeFileSync(filePath, content, 'utf8');
console.log('Fixed extractVideoDirectly in HomeFragment.kt');
