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
import admin from "../fixtures/admin.json"
import tokens from "../fixtures/tokens.json"

test.beforeEach(async ({ page }) => {
	await page.goto('/')
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: admin }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await page.getByRole('button', { name: 'User Info' }).click();
	await page.getByRole('link', { name: 'Settings' }).click();
	await page.getByText('Access tokens').click()
})

test('access tokens forbidden', async ({ page }) => {
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/tokens', async (route) => route.fulfill({ status: 403 }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText('Request failed: GET http://localhost:8080/user/tokens (Forbidden)', { exact: true })).toBeVisible()
})

test('get access tokens', async ({ page }) => {
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/tokens', async (route) => route.fulfill({ json: tokens }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText('test', { exact: true })).toBeVisible()
	await expect(page.getByText('publish', { exact: true })).toBeVisible()
	await expect(page.getByText('pat', { exact: true })).toBeVisible()
})

test('delete access token error', async ({ page }) => {
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'delete-token-csrf' } }))
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/tokens', async (route) => route.fulfill({ json: tokens }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await page.getByRole('button', { name: 'Delete', exact: true }).nth(2).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/token/delete/1254', async (route) => route.fulfill({ status: 403 }))
	await expect(page.getByText('Request failed: POST http://localhost:8080/user/token/delete/1254 (Forbidden)')).toBeVisible()
	await page.getByRole('button', { name: 'Close', exact: true }).click()
	for (let i = 0; i < tokens.length; i++) {
		await expect(page.getByRole('button', { name: 'Delete', exact: true }).nth(i)).toBeDisabled()
	}
	await expect(page.getByRole('progressbar')).toBeVisible()
})

test('delete access token', async ({ page }) => {
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'delete-token-csrf' } }))
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/tokens', async (route) => route.fulfill({ json: tokens }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await page.getByRole('button', { name: 'Delete', exact: true }).nth(2).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/token/delete/1254', async (route) => route.fulfill({ json: { succes: "Deleted access token" } }))
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/tokens', async (route) => route.fulfill({ json: tokens.filter((t) => t.id !== 1253) }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
})

test('delete all access tokens error', async ({ page }) => {
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'delete-token-csrf' } }))
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/tokens', async (route) => route.fulfill({ json: tokens }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await page.getByRole('button', { name: 'Delete all', exact: true }).click()
	await page.getByRole('button', { name: 'Delete', exact: true }).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/token/delete/1252', async (route) => route.fulfill({ status: 404 }))
	await expect(page.getByText('Request failed: POST http://localhost:8080/user/token/delete/1252 (Not Found)')).toBeVisible()
	await page.getByRole('button', { name: 'Close', exact: true }).click()
	for (let i = 0; i < tokens.length; i++) {
		await expect(page.getByRole('button', { name: 'Delete', exact: true }).nth(i)).toBeDisabled()
	}
	await expect(page.getByRole('progressbar')).toBeVisible()
})

test('delete all access tokens', async ({ page }) => {
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'delete-token-csrf' } }))
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/tokens', async (route) => route.fulfill({ json: tokens }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await page.getByRole('button', { name: 'Delete all', exact: true }).click()
	await page.getByRole('button', { name: 'Delete', exact: true }).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/token/delete/1252', async (route) => route.fulfill({ json: { succes: "Deleted access token" } }))
	await page.route('http://localhost:8080/user/token/delete/1253', async (route) => route.fulfill({ json: { succes: "Deleted access token" } }))
	await page.route('http://localhost:8080/user/token/delete/1254', async (route) => route.fulfill({ json: { succes: "Deleted access token" } }))
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/tokens', async (route) => route.fulfill({ json: [] }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText('You currently have no tokens.')).toBeVisible()
})

test('generate token', async ({ page }) => {
	const newToken = {
		id: 1255,
		createdTimestamp: "2025-12-21T18:53:04.100397Z",
		description: "token",
		deleteTokenUrl: "http://localhost:8080/user/token/delete/1255",
		value: "salkdjfsakl-dafkjdalkfdjakl-dasjfdal"
	}

	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'generate-token-csrf' } }))
	await expect(page.getByRole('progressbar')).toBeVisible()
	const responses = [[...tokens, newToken], tokens]
	await page.route('http://localhost:8080/user/tokens', async (route) => {
		await new Promise((resolve) => setTimeout(() => resolve(responses.pop()), 3000)).then(json => route.fulfill({json}));
	})
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await page.getByRole('button', { name: 'Generate new token', exact: true }).click()
	await page.getByLabel('Token Description').fill('token')
	await page.getByRole('button', { name: 'Generate Token', exact: true }).click()
	await expect(page.getByRole('button', { name: 'Generate Token', exact: true })).toBeDisabled()
	await page.route('http://localhost:8080/user/token/create?description=token', route => route.fulfill({ status: 201, json: newToken }))
	await expect(page.getByLabel('Generated Token')).toHaveValue(newToken.value)
	await page.getByRole('button', { name: 'Copied to clipboard!' }).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await expect(page.getByText('test', { exact: true })).toBeVisible()
	await expect(page.getByText('publish', { exact: true })).toBeVisible()
	await expect(page.getByText('pat', { exact: true })).toBeVisible()
	await expect(page.getByText('token', { exact: true })).toBeVisible()
	await expect(page.getByRole('progressbar')).not.toBeVisible()
})

test('generate token error', async ({ page }) => {
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'generate-token-csrf' } }))
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/tokens', async (route) => {
		await new Promise(f => setTimeout(f, 2000));
		await route.fulfill({ json: tokens })
	})
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await page.getByRole('button', { name: 'Generate new token', exact: true }).click()
	await page.getByLabel('Token Description').fill('token')
	await page.getByRole('button', { name: 'Generate Token', exact: true }).click()
	await expect(page.getByRole('button', { name: 'Generate Token', exact: true })).toBeDisabled()
	await page.route('http://localhost:8080/user/token/create?description=token', route => route.fulfill({ status: 400, json: { error: 'The description must not be longer than' } }))

	await expect(page.getByText('The description must not be longer than')).toBeVisible()
	await page.getByRole('button', { name: 'Close', exact: true }).click()
	await expect(page.getByRole('button', { name: 'Generate Token', exact: true })).toBeDisabled()
})