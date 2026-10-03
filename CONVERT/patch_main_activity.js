const fs = require('fs');

const filePath = 'd:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/MainActivity.kt';
let content = fs.readFileSync(filePath, 'utf8');

content = content.replace(
  'bottomNavigation.selectedItemId = R.id.nav_home\n            homeFragment.handleUserUrl(cleanUrl)',
  'bottomNavigation.selectedItemId = R.id.nav_home\n            switchFragment(homeFragment)\n            homeFragment.handleUserUrl(cleanUrl)'
);

fs.writeFileSync(filePath, content, 'utf8');
console.log('Successfully patched MainActivity.kt');
