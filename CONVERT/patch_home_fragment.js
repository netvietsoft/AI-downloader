const fs = require('fs');

const filePath = 'd:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/browser/HomeFragment.kt';
let content = fs.readFileSync(filePath, 'utf8');

// 1. Add pendingUrl declaration
if (!content.includes('private var pendingUrl: String? = null')) {
  content = content.replace(
    'private var currentWebTitle: String? = null',
    'private var currentWebTitle: String? = null\n    private var pendingUrl: String? = null'
  );
}

// 2. Trigger pendingUrl in onViewCreated
if (!content.includes('pendingUrl?.let')) {
  content = content.replace(
    'setupShortcuts(view)',
    'setupShortcuts(view)\n\n        pendingUrl?.let {\n            val toLoad = it\n            pendingUrl = null\n            handleUserUrl(toLoad)\n        }'
  );
}

// 3. Improve getCurrentUrl()
content = content.replace(
  /fun getCurrentUrl\(\): String\? \{[\s\S]*?\}/,
  `fun getCurrentUrl(): String? {
        val inputUrl = if (::etUrlInput.isInitialized) etUrlInput.text.toString().trim().takeIf { it.startsWith("http") } else null
        return inputUrl ?: if (::webView.isInitialized) webView.url else null
    }`
);

// 4. In handleUserUrl check webView initialization
if (!content.includes('if (!::webView.isInitialized)')) {
  content = content.replace(
    'val targetUrl: String = when {',
    'if (!::webView.isInitialized) {\n            pendingUrl = rawInput\n            return\n        }\n\n        val targetUrl: String = when {'
  );
}

fs.writeFileSync(filePath, content, 'utf8');
console.log('Successfully patched HomeFragment.kt');
