const dns = require('dns');
dns.setServers(['1.1.1.1']);
dns.resolve4('www.eporner.com', (err, ips) => {
  if (err) return console.error('DNS Error:', err);
  const ip = ips[0];
  const https = require('https');
  const req = https.request({
    host: ip,
    port: 443,
    path: '/search/dance/',
    method: 'GET',
    servername: 'www.eporner.com',
    headers: {
      'Host': 'www.eporner.com',
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    }
  }, (res) => {
    let data = '';
    res.on('data', c => data += c);
    res.on('end', () => {
      const regex = /\/video-[a-zA-Z0-9_-]+\/[^"'\s<>]+/g;
      const matches = data.match(regex) || [];
      const unique = Array.from(new Set(matches));
      console.log('Status:', res.statusCode);
      console.log('Found video links count:', unique.length);
      unique.slice(0, 5).forEach(link => console.log('https://www.eporner.com' + link));
    });
  });
  req.on('error', console.error);
  req.end();
});
