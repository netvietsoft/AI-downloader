const fs = require('fs');

function inspect(filename) {
  if (!fs.existsSync(filename)) return;
  const xml = fs.readFileSync(filename, 'utf8');
  const texts = [...xml.matchAll(/text="([^"]+)"/g)].map(m => m[1]).filter(Boolean);
  const ids = [...new Set([...xml.matchAll(/resource-id="([^"]+)"/g)].map(m => m[1]).filter(Boolean))];
  console.log(`\n=== FILE: ${filename} ===`);
  console.log('Texts:', texts);
  console.log('IDs:', ids);
}

inspect('ui_e2e_modal1.xml');
inspect('ui_e2e_progress.xml');
inspect('ui_e2e_paymentwall.xml');
