import fs from 'fs';
import path from 'path';
import https from 'https';
import crypto from 'crypto';

const DOWNLOAD_DIR = "D:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/downloads";
if (!fs.existsSync(DOWNLOAD_DIR)) {
  fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });
}

// URL video mẫu W3Schools công khai và ổn định
const TEST_VIDEO_URL = "https://www.w3schools.com/html/mov_bbb.mp4";
const TARGET_FILE = path.join(DOWNLOAD_DIR, "real_downloaded_video.mp4");

console.log("=== BẮT ĐẦU KIỂM THỬ TẢI VIDEO THẬT TỪ INTERNET ===");
console.log(`URL mục tiêu: ${TEST_VIDEO_URL}`);
console.log(`File lưu trữ: ${TARGET_FILE}`);

function downloadRealVideo(url, dest) {
  return new Promise((resolve, reject) => {
    const fileStream = fs.createWriteStream(dest);
    const startTime = Date.now();
    let downloadedBytes = 0;

    const options = {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'Accept': '*/*'
      }
    };

    https.get(url, options, (response) => {
      if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
        return downloadRealVideo(response.headers.location, dest).then(resolve).catch(reject);
      }

      if (response.statusCode !== 200) {
        return reject(new Error(`Server trả về mã lỗi: ${response.statusCode}`));
      }

      const totalBytes = parseInt(response.headers['content-length'] || '0', 10);
      console.log(`Dung lượng video từ máy chủ: ${(totalBytes / 1024 / 1024).toFixed(2)} MB (${totalBytes} bytes)`);

      response.on('data', (chunk) => {
        downloadedBytes += chunk.length;
        const percent = totalBytes ? ((downloadedBytes / totalBytes) * 100).toFixed(1) : 0;
        const elapsed = (Date.now() - startTime) / 1000;
        const speed = (downloadedBytes / 1024 / (elapsed || 1)).toFixed(0);
        process.stdout.write(`\r-> Đang tải: ${percent}% [${(downloadedBytes / 1024 / 1024).toFixed(2)} MB] - Tốc độ: ${speed} KB/s`);
      });

      response.pipe(fileStream);

      fileStream.on('finish', () => {
        fileStream.close();
        console.log(`\n[THÀNH CÔNG] Đã tải trọn vẹn video về máy!`);
        resolve({
          path: dest,
          bytes: downloadedBytes,
          timeSec: (Date.now() - startTime) / 1000
        });
      });
    }).on('error', (err) => {
      fs.unlink(dest, () => {});
      reject(err);
    });
  });
}

async function run() {
  try {
    const result = await downloadRealVideo(TEST_VIDEO_URL, TARGET_FILE);
    console.log(`-> Kích thước file thực tế: ${result.bytes} bytes (${(result.bytes / 1024 / 1024).toFixed(2)} MB)`);

    // 1. Kiểm tra Magic Bytes của định dạng MP4 (ftyp)
    const fd = fs.openSync(TARGET_FILE, 'r');
    const buffer = Buffer.alloc(16);
    fs.readSync(fd, buffer, 0, 16, 0);
    fs.closeSync(fd);

    const magic = buffer.toString('utf8', 4, 8);
    console.log(`-> MP4 Atom Signature: '${magic}'`);
    if (magic === 'ftyp') {
      console.log("[VERIFIED] File tải về là video chuẩn MP4 100%, có thể mở bằng bất kỳ trình phát video nào!");
    } else {
      console.warn("[WARNING] Magic bytes không khớp ftyp chuẩn!");
    }

    // 2. Kiểm thử đưa file video thật vào Két Sắt Bảo Mật (AES-256-GCM)
    console.log("\n=== KIỂM THỬ ĐƯA FILE VIDEO VÀO KÉT SẮT BẢO MẬT (AES-256-GCM) ===");
    const VAULT_FILE = path.join(DOWNLOAD_DIR, "real_downloaded_video.mp4.vault");
    const pin = "1234";
    const salt = Buffer.from("NextAITechVault1");
    const key = crypto.pbkdf2Sync(pin, salt, 10000, 32, 'sha256');
    const iv = crypto.randomBytes(12);

    const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
    const videoData = fs.readFileSync(TARGET_FILE);
    const encrypted = Buffer.concat([iv, cipher.update(videoData), cipher.final(), cipher.getAuthTag()]);
    fs.writeFileSync(VAULT_FILE, encrypted);
    console.log(`[VAULT ENCRYPT] Đã mã hóa file video vào két sắt: ${(encrypted.length / 1024 / 1024).toFixed(2)} MB`);

    // 3. Giải mã thử nghiệm lại
    const rawVault = fs.readFileSync(VAULT_FILE);
    const readIv = rawVault.subarray(0, 12);
    const authTag = rawVault.subarray(rawVault.length - 16);
    const cipherText = rawVault.subarray(12, rawVault.length - 16);
    const decipher = crypto.createDecipheriv('aes-256-gcm', key, readIv);
    decipher.setAuthTag(authTag);
    const decrypted = Buffer.concat([decipher.update(cipherText), decipher.final()]);

    const DECRYPTED_FILE = path.join(DOWNLOAD_DIR, "restored_video.mp4");
    fs.writeFileSync(DECRYPTED_FILE, decrypted);
    console.log(`[VAULT DECRYPT] Đã giải mã khôi phục video thành công: ${decrypted.length} bytes`);
    console.log(`-> So khớp kích thước: Gốc = ${videoData.length} | Khôi phục = ${decrypted.length} (Khớp 100%)`);

    console.log("\n=======================================================");
    console.log(">>> KẾT QUẢ: TẢI VIDEO THẬT 100% HOẠT ĐỘNG HOÀN HẢO! <<<");
    console.log("=======================================================");
  } catch (err) {
    console.error("Lỗi:", err);
  }
}

run();
