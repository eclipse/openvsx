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
import user from "../fixtures/user.json"

test.beforeEach(async ({ page }) => {
	await page.goto('/')
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await page.getByRole('button', { name: 'User Info' }).click();
	await page.getByRole('link', { name: 'Settings' }).click();
})

test('publisher agreement not found', async ({page}) => {
	await page.route('http://localhost:8080/documents/publisher-agreement.md', route => route.fulfill({ status: 404 }))
	await page.getByText('Show publisher agreement').click()
	await expect(page.getByText('Request failed: GET http://localhost:8080/documents/publisher-agreement.md (Not Found)')).toBeVisible()
})

test('sign publisher agreement', async ({ page }) => {
	const signed = { json: { ...user, publisherAgreement: { status: "signed" } } }
	const responses = [signed, { status: 403 }]
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'sign-agreement-csrf' } }))
	await page.route('http://localhost:8080/user/publisher-agreement', async (route) => { route.fulfill(responses.pop()) })
	await page.route('http://localhost:8080/user', async (route) => route.fulfill(signed))
	await page.route('http://localhost:8080/documents/publisher-agreement.md', route => route.fulfill({ body: '# Publisher Agreement' }))
	await page.getByText('Show publisher agreement').click()
	await expect(page.getByRole('heading', { name: 'Publisher Agreement', exact: true })).toBeVisible()
	await page.getByRole('button', { name: 'Agree', exact: true }).click()
	await expect(page.getByRole('button', { name: 'Agree', exact: true })).toBeDisabled()
	await expect(page.getByText('Request failed: POST http://localhost:8080/user/publisher-agreement (Forbidden)Please contact webmaster@eclipse.org if this problem persists.')).toBeVisible()
	await page.getByRole('button', { name: 'Close', exact: true }).click()
	await page.getByRole('button', { name: 'Agree', exact: true }).click()
	await expect(page.getByRole('button', { name: 'Agree', exact: true })).toBeDisabled()
	await expect(page.getByRole('button', { name: 'Agree', exact: true })).not.toBeVisible()
	await expect(page.getByText('You signed the Eclipse Foundation Open VSX Publisher Agreement.')).toBeVisible()
})