const fs = require('fs');
const xml = fs.readFileSync('C:/Users/boluc/.gemini/antigravity-ide/brain/2db2b65d-8ca0-4569-87fb-9b4c625ad148/p_dump.xml', 'utf8');
const regex = /<node[^>]+text="([^"]+)"[^>]+>/g;
let m;
while ((m = regex.exec(xml)) !== null) {
    console.log(m[1]);
}
