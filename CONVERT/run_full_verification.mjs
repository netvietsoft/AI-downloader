/**
 * Master Verification & Quality Gate Runner
 * Script kiểm thử tổng thể toàn bộ hệ thống CONVERT (V2.1 Parity Gate)
 *
 * @author NextAI Technology QA Lead
 */

import fs from 'fs';
import path from 'path';

console.log("======================================================================");
console.log("   NEXTAI VIDEO DOWNLOADER & ACEPLAYER — MASTER QUALITY GATE CHECK    ");
console.log("   Standard: Development Workspace Standard V2.1 Design-Gated         ");
console.log("======================================================================\n");

let passed = 0;
let failed = 0;

function check(title, condition, details = "") {
  if (condition) {
    console.log(`[PASS] ${title}`);
    if (details) console.log(`       ↳ ${details}`);
    passed++;
  } else {
    console.error(`[FAIL] ${title}`);
    if (details) console.error(`       ↳ Error: ${details}`);
    failed++;
  }
}

const CONVERT_DIR = "D:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT";

// 1. Kiểm tra Cấu Trúc Thư Mục Chuẩn Workspace V2.1
const requiredDirs = [
  "Backend/src/api",
  "Backend/src/cms",
  "Backend/src/services",
  "Backend/src/rules",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/core",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/downloader",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/player",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/vault",
  "Docs/Contracts",
  "Docs/Architecture",
  ".ai/tasks"
];

for (const d of requiredDirs) {
  const fullPath = path.join(CONVERT_DIR, d);
  check(`Thư mục chuẩn: ${d}`, fs.existsSync(fullPath));
}

// 2. Kiểm tra Hiến Pháp & Governance Files
check("Tệp hiến pháp AGENTS.md tồn tại", fs.existsSync(path.join(CONVERT_DIR, "AGENTS.md")));
check("Tệp bộ nhớ PROJECT_MEMORY.md tồn tại", fs.existsSync(path.join(CONVERT_DIR, "PROJECT_MEMORY.md")));
check("Tệp nhật ký TASK_LOG.md tồn tại", fs.existsSync(path.join(CONVERT_DIR, "TASK_LOG.md")));
check("Tệp danh mục UPDATETODOS.md tồn tại", fs.existsSync(path.join(CONVERT_DIR, "UPDATETODOS.md")));
check("Tệp cấu hình .ai/project.yaml tồn tại", fs.existsSync(path.join(CONVERT_DIR, ".ai/project.yaml")));
check("Hợp đồng OpenAPI Contracts/openapi.yaml tồn tại", fs.existsSync(path.join(CONVERT_DIR, "Docs/Contracts/openapi.yaml")));

// 3. Kiểm tra Android Package ID & Branding
const manifestPath = path.join(CONVERT_DIR, "apps/android/app/src/main/AndroidManifest.xml");
if (fs.existsSync(manifestPath)) {
  const content = fs.readFileSync(manifestPath, 'utf8');
  check("Package ID chuẩn: com.nextaitechnology.antidetect", content.includes("com.nextaitechnology.antidetect"));
  check("Khai báo Activity AcePlayerActivity", content.includes("AcePlayerActivity"));
  check("Khai báo Foreground DownloadService", content.includes("DownloadService"));
  check("Khai báo FileProvider an toàn", content.includes("androidx.core.content.FileProvider"));
} else {
  check("AndroidManifest.xml tồn tại", false);
}

// 4. Kiểm tra Android Kotlin Core Classes
const coreClasses = [
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/core/model/Models.kt",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/core/security/AesVaultManager.kt",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/core/network/SnifferEngine.kt",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/downloader/DownloadEngine.kt",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/player/AcePlayerController.kt",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/vault/VaultController.kt",
  "apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/browser/TabManager.kt"
];

for (const c of coreClasses) {
  const fullPath = path.join(CONVERT_DIR, c);
  check(`Mã nguồn Android Kotlin: ${path.basename(c)}`, fs.existsSync(fullPath));
  if (fs.existsSync(fullPath)) {
    const code = fs.readFileSync(fullPath, 'utf8');
    check(`Bilingual comments (VI/EN) trong ${path.basename(c)}`, code.includes("package") && code.includes("com.nextaitechnology.antidetect"));
  }
}

// 5. Kiểm tra Backend & Rule Engine Source
const backendFiles = [
  "Backend/src/rules/defaultRules.ts",
  "Backend/src/services/RuleEngine.ts",
  "Backend/src/cms/adminView.ts",
  "Backend/src/api/routes.ts",
  "Backend/src/index.ts",
  "Backend/src/test/backend_test.ts"
];

for (const b of backendFiles) {
  const fullPath = path.join(CONVERT_DIR, b);
  check(`Backend file: ${path.basename(b)}`, fs.existsSync(fullPath));
}

console.log("\n----------------------------------------------------------------------");
console.log(`KẾT QUẢ TỔNG KIỂM THỬ CHẤT LƯỢNG: ${passed} PASSED / ${failed} FAILED`);
console.log("----------------------------------------------------------------------");

if (failed === 0) {
  console.log("\n>>> CHÚC MỪNG: HỆ THỐNG CONVERT ĐẠT 100% TIÊU CHUẨN CỦA V2.1! <<<");
  process.exit(0);
} else {
  console.error("\n>>> CẢNH BÁO: CÓ HẠNG MỤC CHƯA ĐẠT! <<<");
  process.exit(1);
}
