import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

const ADB = '"C:\\Users\\boluc\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe"';
const DEVICE = '192.168.1.18:34749';
const WORK_DIR = "D:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT";

function runAdb(cmd, ignoreError = false) {
  try {
    return execSync(`${ADB} -s ${DEVICE} ${cmd}`, { encoding: 'utf8', timeout: 35000 });
  } catch (e) {
    if (!ignoreError) {
      console.error(`ADB command error: ${cmd} ->`, e.message);
    }
    return e.stdout || e.stderr || '';
  }
}

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

function takeScreenshot(filename) {
  const localPath = path.join(WORK_DIR, filename);
  runAdb(`shell screencap -p /sdcard/${filename}`);
  runAdb(`pull /sdcard/${filename} "${localPath}"`);
  console.log(`[PROOF SCREENSHOT] Saved: ${localPath} (${fs.existsSync(localPath) ? fs.statSync(localPath).size : 0} bytes)`);
}

function dumpUi(filename) {
  const localPath = path.join(WORK_DIR, filename);
  runAdb(`shell uiautomator dump /sdcard/${filename}`);
  runAdb(`pull /sdcard/${filename} "${localPath}"`);
  console.log(`[PROOF UI DUMP] Dumped: ${localPath}`);
}

async function main() {
  console.log("=== BẮT ĐẦU KIỂM THỬ XÁC MINH DANH SÁCH 1822 WEB VÀ PAYMENT WALL GATE ===");

  // 1. Kết nối ADB
  console.log("1. Kết nối thiết bị qua ADB:", DEVICE);
  try {
    execSync(`${ADB} connect ${DEVICE}`, { encoding: 'utf8' });
  } catch (e) {}
  
  const devices = execSync(`${ADB} devices`, { encoding: 'utf8' });
  console.log("Devices:\n" + devices);

  // 2. Cài đặt APK
  const apkPath = path.join(WORK_DIR, "apps/android/app/build/outputs/apk/debug/app-debug.apk");
  console.log("2. Cài đặt APK mới:", apkPath);
  const installOutput = runAdb(`install -r "${apkPath}"`);
  console.log("Install output:", installOutput.trim());

  // 3. Clear app data để đưa về trạng thái sạch (Free user, 0 download)
  console.log("\n3. Reset SharedPreferences/App Data (Đưa tài khoản về 0 lượt tải)...");
  runAdb(`shell pm clear com.nextaitechnology.antidetect`);
  await sleep(2500);

  // 4. Khởi chạy ứng dụng lần đầu
  console.log("4. Khởi động ứng dụng MainActivity...");
  runAdb(`shell pm grant com.nextaitechnology.antidetect android.permission.POST_NOTIFICATIONS`, true);
  runAdb(`shell am start -n com.nextaitechnology.antidetect/.MainActivity`);
  await sleep(4000);
  takeScreenshot("screen_step1_home.png");

  // 5. TEST VIDEO 1: Web thuộc danh sách 1822 domain -> Cho phép tải 1 video đầu tiên
  console.log("\n5. KIỂM THỬ LƯỢT 1: Nhập domain trong 1822 web (xnxx2.com)...");
  const url1 = "https://www.xnxx2.com/video-1f945965/chudai_video_gratis_on_pornhub_video_complete_30_min_in_mi";
  runAdb(`shell am start -n com.nextaitechnology.antidetect/.MainActivity --es EXTRA_URL "${url1}"`);
  
  console.log("-> Đang tải trang và trích xuất video trực tiếp...");
  await sleep(6000);
  takeScreenshot("screen_step2_video1_extracted.png");
  dumpUi("ui_video1.xml");

  // Kiểm tra logcat hoặc giao diện modal download
  console.log("-> Kiểm tra hộp thoại Tải Video hoặc nút Download...");
  // Nhấn nút Tải Về trong dialog/modal nếu đang hiển thị
  // Tọa độ nút Download thường ở giữa phía dưới
  runAdb(`shell input tap 540 1800`, true);
  await sleep(3000);
  takeScreenshot("screen_step3_video1_download_started.png");

  // Kiểm tra Tab Progress để xem tiến trình tải
  console.log("-> Chuyển sang Tab Progress để kiểm tra tiến trình video 1...");
  // Tọa độ tab Progress trên BottomNavigation: khoảng x=380, y=2250 (màn hình 1080x2400 hoặc 720x1600)
  // Hãy xem bounds từ dumpUi hoặc bấm ID
  dumpUi("ui_screen_after_dl1.xml");

  // 6. TEST VIDEO 2: Web thuộc danh sách 1822 domain -> BẬT NGAY PAYMENT WALL
  console.log("\n6. KIỂM THỬ LƯỢT 2: Người dùng nhập domain tiếp theo trong 1822 web -> Phải LẬP TỨC BẬT PAYMENT WALL...");
  const url2 = "https://www.xnxx.com/video-11111/test_monetized_2";
  runAdb(`shell am start -n com.nextaitechnology.antidetect/.MainActivity --es EXTRA_URL "${url2}"`);
  await sleep(4000);

  // Kích hoạt sniffer / tap FAB nếu cần
  takeScreenshot("screen_step4_video2_navigated.png");
  
  // Kiểm tra xem PaymentWallActivity đã bật lên chưa
  await sleep(3000);
  takeScreenshot("screen_step5_video2_paymentwall_gated.png");
  dumpUi("ui_paymentwall_gate.xml");

  // Kiểm tra Activity hiện tại trên thiết bị
  const currentFocus = runAdb(`shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp"`);
  console.log("Current Window Focus:\n" + currentFocus);

  console.log("\n=== HOÀN TẤT KIỂM THỬ TỰ ĐỘNG ===");
}

main().catch(err => console.error("FATAL:", err));
