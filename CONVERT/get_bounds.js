const fs = require('fs');

const xml = fs.readFileSync('ui_video1.xml', 'utf8');
const regex = /<node [^>]*resource-id="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g;
let m;
while ((m = regex.exec(xml)) !== null) {
  const [_, id, x1, y1, x2, y2] = m;
  const cx = Math.floor((parseInt(x1) + parseInt(x2)) / 2);
  const cy = Math.floor((parseInt(y1) + parseInt(y2)) / 2);
  console.log(`${id}: center (${cx}, ${cy}), bounds [${x1},${y1}][${x2},${y2}]`);
}
