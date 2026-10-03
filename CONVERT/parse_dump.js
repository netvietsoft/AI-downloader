const fs = require('fs');

function inspectDump(filename) {
  if (!fs.existsSync(filename)) {
    console.log("File not found:", filename);
    return;
  }
  const xml = fs.readFileSync(filename, 'utf8');
  const texts = [...xml.matchAll(/text="([^"]+)"/g)].map(m => m[1]).filter(Boolean);
  const ids = [...xml.matchAll(/resource-id="([^"]+)"/g)].map(m => m[1]).filter(Boolean);
  console.log(`=== ${filename} ===`);
  console.log("Texts:", texts);
  console.log("IDs:", [...new Set(ids)]);
}

inspectDump('ui_video1.xml');
inspectDump('ui_screen_after_dl1.xml');
inspectDump('ui_paymentwall_gate.xml');
