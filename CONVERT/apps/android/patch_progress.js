const fs = require('fs');
const file = 'D:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/downloader/ProgressFragment.kt';
let code = fs.readFileSync(file, 'utf8');

code = code.replace(
    /\.setMessage\([\s\S]*?\.setPositiveButton/,
    `.setMessage("Ban co chac chan muon " + actionText + ":\\n" + videoTitle + "?")\n            .setPositiveButton`
);

fs.writeFileSync(file, code, 'utf8');
console.log('ProgressFragment.kt patched successfully');
