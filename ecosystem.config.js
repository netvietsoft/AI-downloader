module.exports = {
  apps: [{
    name: 'ai-downloader-web',
    script: 'server.js',
    cwd: '/home/netviet/projects-deploy/ai-dowloader',
    instances: 'max',
    exec_mode: 'cluster',
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
