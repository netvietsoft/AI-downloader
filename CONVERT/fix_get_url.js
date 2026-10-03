const fs = require('fs');

const filePath = 'd:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/browser/HomeFragment.kt';
let content = fs.readFileSync(filePath, 'utf8');

content = content.replace(
  `    fun getCurrentUrl(): String? {
        val inputUrl = if (::etUrlInput.isInitialized) etUrlInput.text.toString().trim().takeIf { it.startsWith("http") } else null
        return inputUrl ?: if (::webView.isInitialized) webView.url else null
    } else null
    }`,
  `    fun getCurrentUrl(): String? {
        val inputUrl = if (::etUrlInput.isInitialized) etUrlInput.text.toString().trim().takeIf { it.startsWith("http") } else null
        return inputUrl ?: if (::webView.isInitialized) webView.url else null
    }`
);

fs.writeFileSync(filePath, content, 'utf8');
console.log('Fixed getCurrentUrl in HomeFragment.kt');
