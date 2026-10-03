import fs from 'fs';
const file = 'CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/browser/HomeFragment.kt';
let content = fs.readFileSync(file, 'utf8');

const target = `        val isVideoUrl = (lower.contains(".m3u8") || lower.contains(".mpd") ||
            (lower.contains(".mp4") && !lower.contains(".m4s")) ||
            (lower.contains("video.") && lower.contains(".fbcdn.net") && lower.contains(".mp4")) ||
            lower.contains("googlevideo.com/videoplayback") ||
            lower.contains("tiktokcdn.com") ||
            lower.contains("xhcdn.com") ||
            lower.contains("xvideos-cdn.com") ||
            lower.contains("phncdn.com")) &&
            !lower.contains(".jpg") && !lower.contains(".png") && !lower.contains(".webp") &&
            !lower.contains(".js") && !lower.contains(".css") && !lower.contains(".svg") &&
            !lower.contains(".gif")`;

const replacement = `        val isVideoUrl = (lower.contains(".m3u8") || lower.contains(".mpd") ||
            (lower.contains(".mp4") && !lower.contains(".m4s")) ||
            (lower.contains("video.") && lower.contains(".fbcdn.net") && lower.contains(".mp4")) ||
            lower.contains("googlevideo.com/videoplayback") ||
            (lower.contains("tiktokcdn.com") && lower.contains(".mp4")) ||
            (lower.contains("xhcdn.com") && (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains("/hls/"))) ||
            (lower.contains("xvideos-cdn.com") && (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains("/hls/"))) ||
            (lower.contains("phncdn.com") && (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains("/hls/")))) &&
            !lower.contains(".jpg") && !lower.contains(".png") && !lower.contains(".webp") &&
            !lower.contains(".js") && !lower.contains(".css") && !lower.contains(".svg") &&
            !lower.contains(".woff") && !lower.contains(".woff2") && !lower.contains(".ttf") &&
            !lower.contains(".gif")`;

const normContent = content.replace(/\r\n/g, '\n');
const normTarget = target.replace(/\r\n/g, '\n');
const normReplacement = replacement.replace(/\r\n/g, '\n');

if (normContent.includes(normTarget)) {
  const updated = normContent.replace(normTarget, normReplacement);
  fs.writeFileSync(file, updated, 'utf8');
  console.log('SUCCESS: Updated HomeFragment.kt with font filters!');
} else {
  console.log('FAILED: Target not found');
}
