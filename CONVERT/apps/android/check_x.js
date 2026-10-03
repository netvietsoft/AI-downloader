const fs = require('fs');
const xml = fs.readFileSync('C:/Users/boluc/.gemini/antigravity-ide/brain/2db2b65d-8ca0-4569-87fb-9b4c625ad148/search_x_active.xml', 'utf8');
const regex = /<node[^>]+>/g;
let m;
while ((m = regex.exec(xml)) !== null) {
    const s = m[0];
    if (s.includes('btn_clear_url') || s.includes('et_url_input') || s.includes('funny') || s.includes('✕')) {
        console.log(s);
    }
}
