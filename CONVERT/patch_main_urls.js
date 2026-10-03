const fs = require('fs');

const filePath = 'd:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/MainActivity.kt';
let content = fs.readFileSync(filePath, 'utf8');

content = content.replaceAll(
  '(supportFragmentManager.findFragmentByTag("HOME") as? HomeFragment)?.getCurrentUrl() ?: ""',
  'homeFragment.getCurrentUrl() ?: ""'
);

content = content.replaceAll(
  '(supportFragmentManager.findFragmentByTag("HOME") as? HomeFragment)?.getPageTitle()',
  'homeFragment.getPageTitle()'
);

fs.writeFileSync(filePath, content, 'utf8');
console.log('Successfully replaced fragment lookup in MainActivity.kt');
