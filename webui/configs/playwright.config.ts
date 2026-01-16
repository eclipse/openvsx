import { defineConfig, devices } from "@playwright/test";
import { SmokeTestOptions } from "./smoke-test.options";

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig<SmokeTestOptions>({
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: "html",
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: { ...devices["Desktop Chrome"] },
  projects: [
    {
      name: "smoke-test",
      testDir: "../test/e2e",
      use: {
        /* Base URL to use in actions like `await page.goto('/')`. */
        baseURL: "https://open-vsx.org",
        /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
        trace: "on-first-retry",
      }
    },
    {
      name: "api-test",
      testDir: "../test/api",
      use: {
        baseURL: "http://localhost:3000",
      }
    }
  ],
  // can't define webServer for a project: https://github.com/microsoft/playwright/issues/22496
  webServer: {
    command: 'yarn prepare && yarn build:default && yarn start:default',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 60 * 1000
  }
});
