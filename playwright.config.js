const { defineConfig } = require('@playwright/test');

const useManualServers = process.env.PLAYWRIGHT_MANUAL_SERVER === '1';

module.exports = defineConfig({
  testDir: './e2e',
  timeout: 120000,
  expect: {
    timeout: 15000
  },
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:3000',
    headless: true,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  webServer: useManualServers
    ? undefined
    : [
        {
          command: 'mvn spring-boot:run',
          url: 'http://localhost:8081/api/auth/status',
          reuseExistingServer: true,
          timeout: 180000,
          cwd: './backend'
        },
        {
          command: 'powershell -Command "$env:BROWSER=\'none\'; $env:CI=\'true\'; npm start"',
          url: 'http://localhost:3000/login',
          reuseExistingServer: true,
          timeout: 180000,
          cwd: '.'
        }
      ]
});
