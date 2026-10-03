const fs = require("fs");
const path = "d:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/browser/HomeFragment.kt";
let content = fs.readFileSync(path, "utf8");

const idx = content.indexOf("fun extractVideoDirectly");
if (idx === -1) {
    console.error("Not found");
    process.exit(1);
}

const endIdx = content.indexOf("private fun setupShortcuts", idx);
if (endIdx === -1) {
    console.error("End not found");
    process.exit(1);
}

const newCode = `fun extractVideoDirectly(url: String) {
        val platformName = when {
            url.contains("facebook") || url.contains("fb.watch") -> "Facebook"
            url.contains("xnxx") -> "XNXX"
            url.contains("xvideos") -> "XVideos"
            url.contains("pornhub") -> "Pornhub"
            else -> "Video"
        }
        Toast.makeText(context, "⚡ Đang bóc tách video " + platformName + "...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val videoInfo = when {
                url.contains("xnxx") || url.contains("xvideos") -> {
                    snifferEngine.extractXnxxVideo(url)
                }
                url.contains("pornhub") || url.contains("redtube") -> {
                    snifferEngine.extractPornhubVideo(url)
                }
                else -> {
                    snifferEngine.extractFacebookVideo(url)
                }
            }
            if (videoInfo != null && videoInfo.qualities.isNotEmpty()) {
                activity?.runOnUiThread {
                    (activity as? MainActivity)?.notifyVideoInfoDetected(videoInfo)
                }
            } else {
                android.util.Log.w("NextAI_Sniffer", "Direct extraction returned null for: " + url)
            }
        }
    }

    `;

const updated = content.substring(0, idx) + newCode + content.substring(endIdx);
fs.writeFileSync(path, updated, "utf8");
console.log("Successfully replaced extractVideoDirectly!");
