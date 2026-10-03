const fs = require('fs');
const xml = fs.readFileSync('C:/Users/boluc/.gemini/antigravity-ide/brain/2db2b65d-8ca0-4569-87fb-9b4c625ad148/with_text_dump.xml', 'utf8');
const regex = /<node[^>]+>/g;
let m;
while ((m = regex.exec(xml)) !== null) {
    const s = m[0];
    if (s.includes('search_bar') || s.includes('et_url_input') || s.includes('btn_clear_url') || s.includes('btn_go_url') || s.includes('SEARCH') || s.includes('nature') || s.includes('✕')) {
        console.log(s);
    }
}
