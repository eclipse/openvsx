# OpenVSX Smoke Tests

The OpenVSX Smoke Tests are designed to ensure that the most critical functionalities of an OpenVSX instance are operational.

The tests in [`smoke-test.spec.ts`](smoke-test.spec.ts) are designed to be executed against a local instance, a staging instance or the production instance at open-vsx.org.
For now, it just contains one configuration in [`playwright.config.ts`](../../configs/playwright.config.ts), which is configured to run against open-vsx.org directly.

## Setup

Begin by installing all necessary dependencies in the [`webui/`](../..) directory:

```bash
npm install --with-deps
```

## Running Tests

The smoke tests run against the webui and can be executed in various modes, each suitable for different stages of development and debugging:

* *Headless Mode* (default): Use `yarn smoke-tests` in [`webui/`](../..), which runs tests in headless mode. This mode is faster and ideal if you are just interested in the results and for CI/CD environments.
* *Headed Mode*: If you want to observe the tests execution in a browser window, you can run them in headed mode using `yarn smoke-tests --headed`.
* *UI Mode*: Playwright provides a dedicated UI mode with `yarn smoke-tests --ui`. The UI mode provides a visual interactive interface to execute and step through tests.
* *Debugging*: For debugging, Playwright provides a `--debug` mode, which opens the Playwright Inspector for a more in-depth examination of test execution.

More detailed information on running tests can be found in the [Playwright documentation](https://playwright.dev/docs/running-tests) and the [command line options guide](https://playwright.dev/docs/test-cli).

## Customizing Test Execution

To test a specific deployment of OpenVSX, you can override the `baseURL` as well as the options defined in `smoke-test.options.ts` by [parameterizing test configurations](https://playwright.dev/docs/test-parameterize) or by adding additional [test projects](https://playwright.dev/docs/test-projects).

For instance, you can create a file `staging.playwright.config.ts` with a content similar to the code below in order to change the `baseURL`, specify HTTP auth parameters from the environment (`dotenv`), and modify the test options for a particular deployment, such as staging:

```ts
import { defineConfig } from '@playwright/test';
import baseConfig from './playwright.config';
import { SmokeTestOptions } from './smoke-test.options';
import dotenv from 'dotenv';

// Read from default ".env" file.
dotenv.config();

// Alternatively, read from "../my.env" file.
dotenv.config({ path: path.resolve(__dirname, '..', 'my.env') });

export default defineConfig<SmokeTestOptions>({
    ...baseConfig,
    use: {
        ...baseConfig.use,
        // Override the baseURL for the staging environment
        baseURL: 'https://staging.open-vsx.org',
        httpCredentials: {
            username: process.env.USERNAME!,
            password: process.env.PASSWORD!
        },
        // customize the values from `smoke-test.options.ts` if necessary
        minNumberOfExtensions: 10,
        extensionToOpen: {
            searchTerm: 'java',
            heading: 'Language Support for Java'
        }
    }
});
```

To execute the tests with the custom configuration above, use the following command:

```bash
npx playwright test --config=staging.playwright.dev.config.ts 

```
