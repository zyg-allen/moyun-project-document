const fs = require('fs');
const args = process.argv.slice(2);
const [src, dst] = args;
const d = JSON.parse(fs.readFileSync(src, 'utf8'));
fs.writeFileSync(dst, Buffer.from(d.content, 'base64').toString('utf8'));
console.log('written ' + dst);
