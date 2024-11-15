import { defineConfig, devices } from "@playwright/test";
import { SmokeTestOptions } from "./smoke-test.options";
import path from "path";

const resultsDir = path.resolve('./', 'playwright-report')

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig<SmokeTestOptions>({
  testDir: "../test/e2e",
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,


  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
      [
          "html", { outputFolder: `${resultsDir}/playwright-report` }
      ],
      [
          "json", { outputFile: `${resultsDir}/report.json` }
      ],
      [
        "../reporters/prometheus-reporter.ts", { outputFile: `${resultsDir}/report.prom` }
      ]
  ],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: "https://open-vsx.org",
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: "on-first-retry",
  },

  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    }
  ],

});
