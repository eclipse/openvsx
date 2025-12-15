/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
import path from "path"
import { expect, test } from "@playwright/test";
import RegexEscape from "regex-escape";
import response0 from "./fixtures/search-response-0.json"
import response10 from "./fixtures/search-response-10.json"
import response20 from "./fixtures/search-response-20.json"
import user from "./fixtures/user.json"

test.beforeEach(async ({ page }) => {
    await page.goto('/')
})

test('login providers', async ({ page }) => {
    await expect(page.getByText('Publish')).not.toBeVisible()
    await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
    await expect(page.getByText('Publish')).toBeVisible()
})

test('no login providers', async ({ page }) => {
    await expect(page.getByText('Publish')).not.toBeVisible()
    await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { success: "No login providers" } }))
    await expect(page.getByText('Publish')).not.toBeVisible()
})

test('version', async ({ page }) => {
    await expect(page.getByText('Loading version...')).toBeVisible()
    await page.route('http://localhost:8080/api/version', async (route) => route.fulfill({ json: { version: "v0.29.1-post-migration" } }))
    await expect(page.getByText('Loading version...')).not.toBeVisible()
    await expect(page.getByText('Server version: v0.29.1-post-migration')).toBeVisible()
})

test('version error', async ({ page }) => {
    await expect(page.getByText('Loading version...')).toBeVisible()
    await page.route('http://localhost:8080/api/version', async (route) => route.fulfill({ json: { error: "No version configured" } }))
    await expect(page.getByText('Loading version...')).not.toBeVisible()
    await expect(page.getByText('Server version:')).toBeVisible()
})

test('version exception', async ({ page }) => {
    await expect(page.getByText('Loading version...')).toBeVisible()
    await page.route('http://localhost:8080/api/version', async (route) => route.fulfill({ status: 400 }))
    await expect(page.getByText('Loading version...')).not.toBeVisible()
    await expect(page.getByText('Server version: unknown')).toBeVisible()
})

test('search error', async ({ page }) => {
    await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
    await expect(page.getByText('0 Results')).toBeVisible()
    await page.route('http://localhost:8080/api/-/search?size=10&sortBy=relevance&sortOrder=desc', async (route) => route.fulfill({ json: { error: 'Search unavailable' } }))

    await expect(page.getByText('0 Results')).toBeVisible()
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
    await expect(page.getByRole('presentation')).toHaveText(/Search unavailable/)
})

test('search exception', async ({ page }) => {
    await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
    await expect(page.getByText('0 Results')).toBeVisible()
    await page.route('http://localhost:8080/api/-/search?size=10&sortBy=relevance&sortOrder=desc', async (route) => route.fulfill({ status: 400 }))

    await expect(page.getByText('0 Results')).toBeVisible()
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
    await expect(page.getByRole('presentation')).toHaveText(new RegExp(RegexEscape('Request failed: GET http://localhost:8080/api/-/search?size=10&sortBy=relevance&sortOrder=desc (Bad Request)')))
})

test('search', async ({ page }) => {
    await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
    await expect(page.getByText('0 Results')).toBeVisible()
    await page.route('http://localhost:8080/api/-/search?size=10&sortBy=relevance&sortOrder=desc', async (route) => route.fulfill({ json: response0 }))
    await page.route('http://localhost:8080/api/-/search?offset=10&size=10&sortBy=relevance&sortOrder=desc', async (route) => route.fulfill({ json: response10 }))
    await page.route('http://localhost:8080/api/redhat/java/1.51.2025121108/file/icon128.png', async (route) => route.fulfill({ path: path.normalize(__dirname + '/fixtures/icon128.png') }))
    await page.route("http://localhost:8080/api/eamodio/gitlens/2025.12.1519/file/gitlens-icon.png", async (route) => route.fulfill({ status: 404 }))
    await expect(page.getByRole('presentation')).toHaveText(new RegExp(RegexEscape('Request failed: GET http://localhost:8080/api/eamodio/gitlens/2025.12.1519/file/gitlens-icon.png (Not Found)')))
    await page.getByText('Close').click()
    await expect(page.getByRole('presentation')).not.toBeVisible()

    await expect(page.getByText('8349 Results')).toBeVisible()
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
    const container = page.locator('.MuiGrid-container')
    await expect(container.getByRole('link').nth(0).getByRole('img')).not.toHaveAttribute('src', '/default-icon.png')
    for (let i = 0; i < response0.extensions.length; i++) {
        const { displayName } = response0.extensions[i]
        await expect(container.getByRole('link').nth(i)).toHaveText(new RegExp('^' + RegexEscape(displayName)))
    }
    for (let i = 0; i < response10.extensions.length; i++) {
        const { displayName } = response10.extensions[i]
        await expect(container.getByRole('link').nth(10 + i)).toHaveText(new RegExp('^' + RegexEscape(displayName)))
    }

    await page.evaluate(() => window.scrollBy(0, document.body.scrollHeight));
    await page.mouse.wheel(0, -100)
    await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
    await page.route('http://localhost:8080/api/-/search?offset=20&size=10&sortBy=relevance&sortOrder=desc', async (route) => route.fulfill({ json: response20 }))
    for (let i = 0; i < response20.extensions.length; i++) {
        const { displayName } = response20.extensions[i]
        await expect(container.getByRole('link').nth(20 + i)).toHaveText(new RegExp('^' + RegexEscape(displayName)))
    }
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()

    await page.evaluate(() => window.scrollBy(0, document.body.scrollHeight));
    await page.mouse.wheel(0, -100)
    await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
    await page.route('http://localhost:8080/api/-/search?offset=30&size=10&sortBy=relevance&sortOrder=desc', async (route) => route.fulfill({ json: { error: 'Search unavailable' } }))
    await expect(page.getByRole('presentation')).toHaveText(/Search unavailable/)
    await page.getByText('Close').click()
    await expect(page.getByRole('presentation')).not.toBeVisible()
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()

    await page.evaluate(() => window.scrollBy(0, document.body.scrollHeight));
    await page.mouse.wheel(0, -100)
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('logout', async ({ page }) => {
    await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
    await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
    await expect(page.getByText('Publish')).toBeVisible()
    await page.getByRole('button', { name: 'User Info' }).click();
    await page.getByRole('button', { name: 'Log Out' }).click();
})