const fs = require('fs');
const file = process.argv[2] || 'C:/Users/boluc/.gemini/antigravity-ide/brain/2db2b65d-8ca0-4569-87fb-9b4c625ad148/home_real_dump.xml';
const xml = fs.readFileSync(file, 'utf8');
const regex = /<node[^>]+text="([^"]+)"[^>]+>/g;
let m;
while ((m = regex.exec(xml)) !== null) {
    console.log(m[1]);
}
