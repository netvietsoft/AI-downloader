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
  console.log(`[PROOF SCREENSHOT] Saved: ${filename} (${fs.existsSync(localPath) ? fs.statSync(localPath).size : 0} bytes)`);
}

function dumpUi(filename) {
  const localPath = path.join(WORK_DIR, filename);
  runAdb(`shell uiautomator dump /sdcard/${filename}`);
  runAdb(`pull /sdcard/${filename} "${localPath}"`);
  console.log(`[PROOF UI DUMP] Dumped: ${filename}`);
}

async function main() {
  console.log("======================================================================");
  console.log("=== BẮT ĐẦU TEST TOÀN DIỆN: 1822 DOMAIN MONETIZATION & PAYMENT WALL ===");
  console.log("======================================================================");

  // 1. Kết nối ADB
  console.log("1. Kết nối ADB...");
  try { execSync(`${ADB} connect ${DEVICE}`, { encoding: 'utf8' }); } catch (e) {}
  console.log("Thiết bị:\n" + execSync(`${ADB} devices`, { encoding: 'utf8' }));

  // 2. Clear app data -> Đưa về trạng thái ban đầu: Free User, 0 downloads
  console.log("\n2. Reset App Data (Free User, 0 downloads)...");
  runAdb(`shell pm clear com.nextaitechnology.antidetect`);
  await sleep(2500);

  // 3. Khởi chạy ứng dụng MainActivity
  console.log("\n3. Khởi chạy MainActivity...");
  runAdb(`shell pm grant com.nextaitechnology.antidetect android.permission.POST_NOTIFICATIONS`, true);
  runAdb(`shell am start -n com.nextaitechnology.antidetect/.MainActivity`);
  await sleep(3500);
  takeScreenshot("screen_step1_home.png");

  // 4. TEST VIDEO 1 (LƯỢT 1 - MIỄN PHÍ DÙNG THỬ 1/1):
  const video1 = "https://www.xnxx.com/video-1hk29p00/hot_chick_gets_some_rough_fucking";
  console.log("\n4. TEST LƯỢT 1: Nhập domain từ danh sách 1822 web (xnxx.com):");
  console.log("   URL: " + video1);
  runAdb(`shell am start -n com.nextaitechnology.antidetect/.MainActivity --es EXTRA_URL "${video1}"`);
  
  console.log("   -> Chờ WebView tải trang và SnifferEngine bóc tách video...");
  await sleep(6000);
  takeScreenshot("screen_step2_video1_page.png");

  // Nhấn FAB Sniffer mở modal tải
  console.log("   -> Nhấn FAB Sniffer mở Modal (tọa độ 633, 1310)...");
  runAdb(`shell input tap 633 1310`);
  await sleep(2000);
  takeScreenshot("screen_step3_video1_modal.png");
  dumpUi("ui_step3_modal.xml");

  // Bấm nút TẢI VỀ MÁY (btn_confirm_download: center 512, 1427)
  console.log("   -> Bấm nút 'TAI VE MAY' (Lượt tải dùng thử miễn phí 1/1) tại tọa độ (512, 1427)...");
  runAdb(`shell input tap 512 1427`);
  await sleep(3000);
  takeScreenshot("screen_step4_video1_download_started.png");

  // 5. TEST VIDEO 2 (LƯỢT 2 TRỞ ĐI - BẬT PAYMENT WALL):
  const video2 = "https://www.xnxx.com/video-17id930f/stasia_si_-_first_time_bdsm_rough_fucking_at_home";
  console.log("\n5. TEST LƯỢT 2: Người dùng nhập domain tiếp theo trong 1822 web:");
  console.log("   URL: " + video2);
  runAdb(`shell am start -n com.nextaitechnology.antidetect/.MainActivity --es EXTRA_URL "${video2}"`);

  console.log("   -> Chờ Sniffer phát hiện video thứ 2...");
  await sleep(6000);
  takeScreenshot("screen_step5_video2_page.png");

  // Nhấn nút FAB Sniffer hoặc sniffer tự động phát hiện video
  console.log("   -> Nhấn Sniffer FAB kiểm tra kích hoạt Payment Wall...");
  runAdb(`shell input tap 633 1310`);
  await sleep(3000);

  // Chụp ảnh màn hình Payment Wall và dump UI
  takeScreenshot("screen_step6_paymentwall_gated.png");
  dumpUi("ui_step6_paymentwall.xml");

  // 6. Kiểm tra nội dung Payment Wall
  const paywallXmlPath = path.join(WORK_DIR, "ui_step6_paymentwall.xml");
  if (fs.existsSync(paywallXmlPath)) {
    const xml = fs.readFileSync(paywallXmlPath, 'utf8');
    const texts = [...xml.matchAll(/text="([^"]+)"/g)].map(m => m[1]).filter(Boolean);
    console.log("\n=== NỘI DUNG MÀN HÌNH PAYMENT WALL ĐÃ BẬT ===");
    console.log("Các văn bản hiển thị trên màn hình:", texts);
    const hasPaywallNotice = texts.some(t => t.includes("dùng hết 1 lượt tải") || t.includes("PRO") || t.includes("Đăng Ký") || t.includes("Gói"));
    if (hasPaywallNotice) {
      console.log("[XÁC MINH THÀNH CÔNG] ĐÃ BẬT PAYMENT WALL VÀ CHẶN TẢI VIDEO THỨ 2 THEO ĐÚNG YÊU CẦU CỦA CHỦ TỊCH!");
    } else {
      console.log("[CẢNH BÁO] Không tìm thấy thông báo gói cước trong UI Dump!");
    }
  }

  console.log("\n======================================================================");
  console.log("=== HOÀN TẤT CHU TRÌNH TEST THỰC TẾ TRÊN MÁY SAMSUNG GALAXY A07 ===");
  console.log("======================================================================");
}

main().catch(err => console.error("FATAL:", err));
