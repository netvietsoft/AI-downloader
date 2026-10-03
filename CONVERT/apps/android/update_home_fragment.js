const fs = require('fs');

const fragmentPath = 'D:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/browser/HomeFragment.kt';
let code = fs.readFileSync(fragmentPath, 'utf8');

// 1. Add imports if missing
if (!code.includes('import android.text.TextWatcher')) {
    code = code.replace(
        'import android.widget.EditText',
        'import android.text.Editable\nimport android.text.TextWatcher\nimport android.view.inputmethod.EditorInfo\nimport android.view.inputmethod.InputMethodManager\nimport java.net.URLEncoder\nimport android.widget.EditText'
    );
}

// 2. Add btnClearUrl field
if (!code.includes('private lateinit var btnClearUrl: View')) {
    code = code.replace(
        'private lateinit var btnGoUrl: View',
        'private lateinit var btnGoUrl: View\n    private lateinit var btnClearUrl: View'
    );
}

// 3. Initialize btnClearUrl in onViewCreated
if (!code.includes('btnClearUrl = view.findViewById(R.id.btn_clear_url)')) {
    code = code.replace(
        'btnGoUrl = view.findViewById(R.id.btn_go_url)',
        'btnGoUrl = view.findViewById(R.id.btn_go_url)\n        btnClearUrl = view.findViewById(R.id.btn_clear_url)'
    );
}

// 4. Update onPageStarted to update btnClearUrl visibility
code = code.replace(
    /override fun onPageStarted\(view: WebView\?, url: String\?, favicon: Bitmap\?\) \{[\s\S]*?etUrlInput\.setText\(url\)\s*\}/,
    `override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (url != null) {
                        etUrlInput.setText(url)
                        btnClearUrl.visibility = if (url.isNotEmpty()) View.VISIBLE else View.GONE
                    }`
);

// 5. Replace setupSearchBar and handleUserUrl
const oldSetupRegex = /private fun setupSearchBar\(\) \{[\s\S]*?fun handleUserUrl\(rawInput: String\) \{[\s\S]*?loadUrl\(cleanInput\)\s*\}/;

const newSetupCode = `private fun setupSearchBar() {
        // Lắng nghe thay đổi văn bản để hiển thị/ẩn nút '✕' xóa nhanh
        // Listen to text changes to show/hide the clear '✕' button
        etUrlInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearUrl.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Bấm nút '✕' để xóa toàn bộ URL/từ khóa và focus lại ô nhập
        // Tap '✕' button to clear URL/keyword and refocus input
        btnClearUrl.setOnClickListener {
            etUrlInput.text?.clear()
            btnClearUrl.visibility = View.GONE
            etUrlInput.requestFocus()
        }

        // Bấm nút SEARCH hoặc nhấn Enter/Go trên bàn phím ảo
        // Tap SEARCH button or press Enter/Go on soft keyboard
        btnGoUrl.setOnClickListener {
            val input = etUrlInput.text.toString().trim()
            handleUserUrl(input)
            hideKeyboard()
        }

        etUrlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE) {
                val input = etUrlInput.text.toString().trim()
                handleUserUrl(input)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        btnPasteDetect.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (clipText.startsWith("http") || clipText.contains("facebook.com") || clipText.contains("fb.watch")) {
                etUrlInput.setText(clipText)
                handleUserUrl(clipText)
                Toast.makeText(context, "Đã nhận diện liên kết: " + clipText, Toast.LENGTH_SHORT).show()
            } else {
                val demoReel = "https://www.facebook.com/reel/2120496905227881"
                etUrlInput.setText(demoReel)
                handleUserUrl(demoReel)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(etUrlInput.windowToken, 0)
    }

    /**
     * Xử lý liên kết hoặc từ khóa tìm kiếm:
     * - Mặc định công cụ tìm kiếm Google nếu nhập từ khóa, tên website hoặc nội dung cần tìm.
     * - Tự động duyệt web trực tiếp nếu nhập URL hợp lệ.
     *
     * Handle input query or URL:
     * - Defaults to Google Search if the user enters keywords, website names, or search terms.
     * - Directly navigates to site if valid URL is provided.
     */
    fun handleUserUrl(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            loadUrl("https://www.google.com")
            return
        }

        val targetUrl: String = when {
            trimmed.startsWith("fb://fullscreen_video/") -> {
                val vid = trimmed.substringAfter("fullscreen_video/").substringBefore('?')
                "https://www.facebook.com/reel/" + vid
            }
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                trimmed
            }
            // Nếu có khoảng trắng hoặc không có dấu chấm phân cách -> Từ khóa tìm kiếm Google
            trimmed.contains(" ") || !trimmed.contains(".") || trimmed.endsWith(".") -> {
                "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
            }
            // Nếu là định dạng domain hợp lệ (vd: facebook.com, xhamster.desi) -> mở trực tiếp
            android.util.Patterns.WEB_URL.matcher("https://" + trimmed).matches() -> {
                "https://" + trimmed
            }
            else -> {
                "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
            }
        }

        // Nếu là liên kết Facebook Reel / Watch -> Trích xuất trực tiếp song song
        if (targetUrl.contains("facebook.com") || targetUrl.contains("fb.watch")) {
            extractVideoDirectly(targetUrl)
        }

        loadUrl(targetUrl)
    }`;

code = code.replace(oldSetupRegex, newSetupCode);

fs.writeFileSync(fragmentPath, code, 'utf8');
console.log('HomeFragment.kt updated successfully');
