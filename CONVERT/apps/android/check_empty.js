const fs = require('fs');
const xml = fs.readFileSync('C:/Users/boluc/.gemini/antigravity-ide/brain/2db2b65d-8ca0-4569-87fb-9b4c625ad148/empty_dump.xml', 'utf8');
const regex = /<node[^>]+>/g;
let m;
while ((m = regex.exec(xml)) !== null) {
    const s = m[0];
    if (s.includes('tab_') || s.includes('empty') || s.includes('Chưa') || s.includes('Downloaded')) {
        console.log(s);
    }
}
