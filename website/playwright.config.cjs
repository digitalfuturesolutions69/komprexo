const { defineConfig } = require('@playwright/test');
module.exports = defineConfig({
  testDir: './tests',
  timeout: 30000,
  workers: 2,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: { baseURL: 'http://127.0.0.1:4173', headless: true,
    launchOptions: { executablePath: process.env.CHROMIUM_PATH || undefined } },
  webServer: { command: 'python3 -m http.server 4173 --bind 127.0.0.1 --directory build',
    url: 'http://127.0.0.1:4173', reuseExistingServer: false }
});
