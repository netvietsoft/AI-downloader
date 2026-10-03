module.exports = {
  apps: [{
    name: 'ai-downloader-web',
    script: 'server.js',
    cwd: '/home/netviet/projects-deploy/ai-dowloader',
    instances: 1,
    exec_mode: 'fork',
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    env: {
      NODE_ENV: 'production',
      PORT: 3070,
      DOMAIN: 'snaptik2.com'
    }
  }]
};
