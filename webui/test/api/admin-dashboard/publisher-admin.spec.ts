/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
import { expect, test } from "@playwright/test";
import user from "../fixtures/admin.json"
import publisher from "../fixtures/publisher.json"

test.beforeEach(async ({ page }) => {
	await page.goto('/')
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await page.getByRole('button', { name: 'User Info' }).click();
	await page.getByRole('link', { name: 'Admin Dashboard' }).click();
	await page.getByRole('button', { name: 'Publishers', exact: true }).click()
})

test('get publisher info', async ({ page }) => {
	await page.getByPlaceholder('Publisher Name').fill('amvanbaren')
	await page.getByTestId('SearchIcon').click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/publisher/github/amvanbaren', (route) => route.fulfill({ json: publisher }))
	await expect(page.getByText('Login name: amvanbaren')).toBeVisible()
	await expect(page.getByRole('progressbar')).not.toBeVisible()
})

test('publisher info error', async ({ page }) => {
	await page.getByPlaceholder('Publisher Name').fill('amvanbaren')
	await page.getByTestId('SearchIcon').click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/publisher/github/amvanbaren', (route) => route.fulfill({ status: 403 }))
	await expect(page.getByText('Request failed: GET http://localhost:8080/admin/publisher/github/amvanbaren (Forbidden)')).toBeVisible()
	await expect(page.getByRole('progressbar')).not.toBeVisible()
})

test('publisher not found', async ({ page }) => {
	await page.getByPlaceholder('Publisher Name').fill('amvanbaren')
	await page.getByTestId('SearchIcon').click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/publisher/github/amvanbaren', (route) => route.fulfill({ status: 404 }))
	await expect(page.getByText('Publisher amvanbaren not found.')).toBeVisible()
	await expect(page.getByRole('progressbar')).not.toBeVisible()
})

test('revoke publisher tokens', async ({ page }) => {
	const responses = [{ json: { success: 'Deactivated 3 tokens' } }, { status: 404, json: { error: 'User not found' } }]
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'revoke-tokens-csrf' } }))
	await page.route('http://localhost:8080/admin/publisher/github/amvanbaren/tokens/revoke', async (route) => route.fulfill(responses.pop()))

	await page.getByPlaceholder('Publisher Name').fill('amvanbaren')
	await page.getByTestId('SearchIcon').click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	const publishers = [{ ...publisher, activeAccessTokenNum: 0 }, publisher]
	await page.route('http://localhost:8080/admin/publisher/github/amvanbaren', async (route) => {
		await new Promise(f => setTimeout(f, 2000))
		await route.fulfill({ json: publishers.pop() })
	})
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText('3 active access tokens.')).toBeVisible()
	await page.getByText('Revoke access tokens').click()
	await expect(page.getByText('Revoke access tokens')).toBeDisabled()
	await expect(page.getByText('User not found')).toBeVisible()
	await page.getByRole('button', { name: 'Close' }).click()
	await expect(page.getByText('Revoke access tokens')).not.toBeDisabled()

	await page.getByText('Revoke access tokens').click()
	await expect(page.getByText('Revoke access tokens')).toBeDisabled()
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await expect(page.getByText('0 active access tokens.')).toBeVisible()
	await expect(page.getByText('Revoke access tokens')).not.toBeVisible()
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('revoke publisher contributions', async ({ page }) => {
	const responses = [{ json: { success: 'Revoked publisher contributions' } }, { status: 404, json: { error: 'User not found' } }]
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'revoke-tokens-csrf' } }))
	await page.route('http://localhost:8080/admin/publisher/github/amvanbaren/revoke', async (route) => route.fulfill(responses.pop()))

	await page.getByPlaceholder('Publisher Name').fill('amvanbaren')
	await page.getByTestId('SearchIcon').click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	const revokedExtensions = publisher.extensions.map(e => {
		e.active = false
		return e
	})
	const publishers = [{ ...publisher, activeAccessTokenNum: 0, extensions: revokedExtensions}, publisher]
	await page.route('http://localhost:8080/admin/publisher/github/amvanbaren', (route) => route.fulfill({ json: publishers.pop() }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText('3 active access tokens.')).toBeVisible()
	await page.getByText('Revoke publisher contributions').click()
	await page.getByText('Revoke contributions').click()
	await expect(page.getByText('Revoke contributions')).toBeDisabled()
	await expect(page.getByText('User not found')).toBeVisible()
	await page.getByRole('button', { name: 'Close' }).click()
	await expect(page.getByText('Revoke contributions')).not.toBeDisabled()

	await page.getByText('Revoke contributions').click()
	await expect(page.getByText('Revoke contributions')).toBeDisabled()
	await expect(page.getByText('Revoke contributions')).not.toBeVisible()
	await expect(page.getByText('0 active access tokens.')).toBeVisible()
	for(let i = 0; i < publisher.extensions.length; i++) {
		await expect(page.getByText('Deactivated').nth(i)).toBeVisible()
	}
})