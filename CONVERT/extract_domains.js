const fs = require('fs');
const path = require('path');

const csvPath = 'D:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/web/Bao_cao_web_video.csv';
const content = fs.readFileSync(csvPath, 'utf8');

const lines = content.split(/\r?\n/);
const domains = new Set();

function cleanDomain(raw) {
    if (!raw) return '';
    let d = raw.trim().toLowerCase();
    d = d.replace(/^https?:\/\//, '');
    d = d.replace(/^www\./, '');
    d = d.split('/')[0];
    d = d.split('?')[0];
    d = d.split(':')[0];
    return d.trim();
}

for (let i = 1; i < lines.length; i++) {
    const line = lines[i];
    if (!line.trim()) continue;

    // Standard CSV split handling quotes
    const cols = [];
    let current = '';
    let inQuotes = false;
    for (let c = 0; c < line.length; c++) {
        const char = line[c];
        if (char === '"') {
            inQuotes = !inQuotes;
        } else if (char === ',' && !inQuotes) {
            cols.push(current);
            current = '';
        } else {
            current += char;
        }
    }
    cols.push(current);

    if (cols.length > 1) {
        const d = cleanDomain(cols[1]);
        if (d && d !== 'domain' && d.includes('.')) {
            domains.add(d);
        }
    }
}

const sortedDomains = Array.from(domains).sort();
console.log(`Extracted ${sortedDomains.length} unique domains.`);

const assetsDir = path.join(__dirname, 'apps/android/app/src/main/assets');
if (!fs.existsSync(assetsDir)) {
    fs.mkdirSync(assetsDir, { recursive: true });
}

const outputPath = path.join(assetsDir, 'monetized_domains.txt');
fs.writeFileSync(outputPath, sortedDomains.join('\n'), 'utf8');
console.log(`Successfully wrote ${sortedDomains.length} domains to: ${outputPath}`);
