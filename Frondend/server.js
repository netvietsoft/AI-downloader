const express = require('express');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = 5100;

const publicDir = path.join(__dirname, 'public');
if (!fs.existsSync(publicDir)) {
    fs.mkdirSync(publicDir, { recursive: true });
}

// Phục vụ các tệp tĩnh
app.use(express.static(publicDir));

// Route phục vụ file zip Extension trực tiếp từ thư mục WWW
app.get('/download-extension', (req, res) => {
    const zipPath = path.join(__dirname, '..', 'ai-downloader-extension.zip');
    if (fs.existsSync(zipPath)) {
        res.download(zipPath, 'AI_Video_Downloader_Extension.zip');
    } else {
        res.status(404).send('File Extension zip chưa sẵn sàng!');
    }
});

// SPA fallback về index.html
app.use((req, res) => {
    res.sendFile(path.join(publicDir, 'index.html'));
});

app.listen(PORT, () => {
    console.log(`=======================================================`);
    console.log(`🌟 AI Downloader Frontend Web App is RUNNING!`);
    console.log(`🌐 Web UI: http://localhost:${PORT}`);
    console.log(`📦 Extension Download: http://localhost:${PORT}/download-extension`);
    console.log(`=======================================================`);
});
