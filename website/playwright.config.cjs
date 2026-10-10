const { defineConfig } = require('@playwright/test');
module.exports = defineConfig({
  testDir: './tests',
  timeout: 30000,
  workers: 2,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: { headless: true,
    launchOptions: { executablePath: process.env.CHROMIUM_PATH || undefined } },
  projects: [
    { name: 'rumahweb-root', use: { baseURL: 'http://127.0.0.1:4173/' } },
    { name: 'historical-project-prefix', use: { baseURL: 'http://127.0.0.1:4174/komprexo/' } }
  ],
  webServer: [
    { command: 'python3 -m http.server 4173 --bind 127.0.0.1 --directory build',
      url: 'http://127.0.0.1:4173', reuseExistingServer: false },
    { command: 'python3 -m http.server 4174 --bind 127.0.0.1 --directory deployment/project-preview',
      url: 'http://127.0.0.1:4174/komprexo/', reuseExistingServer: false }
  ]
});
