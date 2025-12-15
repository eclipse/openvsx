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
import namespace from "../fixtures/namespace-admin.json"
import extension from "../fixtures/namespace-admin-extension.json"
import members from "../fixtures/namespace-admin-members.json"

test.beforeEach(async ({ page }) => {
	await page.goto('/')
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await page.getByRole('button', { name: 'User Info' }).click();
	await page.getByRole('link', { name: 'Admin Dashboard' }).click();
	await page.getByRole('button', { name: 'Namespaces', exact: true }).click()
})

test('get namespace', async ({ page }) => {
	await page.getByPlaceholder('Namespace').fill('vscodevim')
	await page.getByTestId('SearchIcon').click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/namespace/vscodevim', async (route) => route.fulfill({ json: namespace }))
	await page.route('http://localhost:8080/api/vscodevim/vim', async (route) => route.fulfill({ json: extension }))
	await page.route('http://localhost:8080/admin/namespace/vscodevim/members', async (route) => route.fulfill({ json: members }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByRole('heading').first()).toHaveText(namespace.name)
	await expect(page.getByText(members.namespaceMemberships[0].user.loginName)).toBeVisible()
	await expect(page.getByRole('link', { name: `${extension.name} Version: ${extension.version}` })).toBeVisible()
})

test('create namespace', async ({ page }) => {
	const responses = [{ json: { success: 'Namespace created' } }, { status: 400, json: { error: 'Namespace already exists' } }, { status: 403 }]
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'create-namespace-csrf' } }))
	await page.route('http://localhost:8080/admin/create-namespace', async (route) => {
		setTimeout(() => route.fulfill(responses.pop()), 1000)
	})

	await page.getByPlaceholder('Namespace').fill('vscodevim')
	await page.getByTestId('SearchIcon').click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/namespace/vscodevim', async (route) => route.fulfill({ status: 404 }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText(`Namespace ${namespace.name} not found. Do you want to create it?`)).toBeVisible()
	await expect(page.getByRole('button', { name: `Create Namespace ${namespace.name}`, exact: true })).toBeVisible()
	await page.getByRole('button', { name: `Create Namespace ${namespace.name}`, exact: true }).click()
	await expect(page.getByRole('button', { name: `Create Namespace ${namespace.name}`, exact: true })).toBeDisabled()
	await expect(page.getByText('Request failed: POST http://localhost:8080/admin/create-namespace (Forbidden)')).toBeVisible()
	await page.getByRole('presentation').getByRole('button', { name: 'Close', exact: true }).click()

	await page.getByRole('button', { name: `Create Namespace ${namespace.name}`, exact: true }).click()
	await expect(page.getByRole('button', { name: `Create Namespace ${namespace.name}`, exact: true })).toBeDisabled()
	await expect(page.getByText('Namespace already exists')).toBeVisible()
	await page.getByRole('presentation').getByRole('button', { name: 'Close', exact: true }).click()

	await page.route('http://localhost:8080/admin/namespace/vscodevim', async (route) => route.fulfill({ json: namespace }))
	await page.route('http://localhost:8080/api/vscodevim/vim', async (route) => route.fulfill({ json: extension }))
	await page.route('http://localhost:8080/admin/namespace/vscodevim/members', async (route) => route.fulfill({ json: members }))
	await page.getByRole('button', { name: `Create Namespace ${namespace.name}`, exact: true }).click()
	await expect(page.getByRole('button', { name: `Create Namespace ${namespace.name}`, exact: true })).toBeDisabled()
	await expect(page.getByRole('progressbar').first()).toBeVisible()
	await expect(page.getByRole('button', { name: `Create Namespace ${namespace.name}`, exact: true })).not.toBeVisible()
	await expect(page.getByRole('progressbar').first()).not.toBeVisible()
	await expect(page.getByRole('heading').first()).toHaveText(namespace.name)
	await expect(page.getByText(members.namespaceMemberships[0].user.loginName)).toBeVisible()
	await expect(page.getByRole('link', { name: `${extension.name} Version: ${extension.version}` })).toBeVisible()
})

test('namespace error', async ({ page }) => {
	await page.getByPlaceholder('Namespace').fill('vscodevim')
	await page.getByTestId('SearchIcon').click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/namespace/vscodevim', async (route) => route.fulfill({ status: 403 }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText('Request failed: GET http://localhost:8080/admin/namespace/vscodevim (Forbidden)')).toBeVisible()
})

test('change namespace', async ({ page }) => {
	const responses = [{ json: { success: 'Scheduled namespace change' } }, { json: { error: 'New namespace already exists' } }]
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'change-namespace-csrf' } }))
	await page.route('http://localhost:8080/admin/change-namespace', async (route) => {
		await new Promise(f => setTimeout(f, 2500))
		await route.fulfill(responses.pop())
	})

	await page.getByPlaceholder('Namespace').fill('vscodevim')
	await page.getByTestId('SearchIcon').click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/namespace/vscodevim', async (route) => route.fulfill({ json: namespace }))
	await page.route('http://localhost:8080/api/vscodevim/vim', async (route) => route.fulfill({ json: extension }))
	await page.route('http://localhost:8080/admin/namespace/vscodevim/members', async (route) => route.fulfill({ json: members }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await page.getByText('Change namespace').click()
	await page.getByRole('presentation').getByLabel('New Open VSX Namespace').fill('vimium')
	await page.getByRole('presentation').getByRole('button', { name: 'Change namespace' }).click()
	await expect(page.getByRole('presentation').getByRole('button', { name: 'Change namespace' })).toBeDisabled()
	await expect(page.getByRole('presentation')).toHaveText("ErrorNew namespace already existsClose")
	await page.getByRole('presentation').getByRole('button', { name: 'Close' }).click()
	await page.getByRole('presentation').getByRole('button', { name: 'Change namespace' }).click()
	await expect(page.getByRole('presentation').getByRole('button', { name: 'Change namespace' })).toBeDisabled()
	await expect(page.getByRole('presentation')).toHaveText("InfoScheduled namespace changeClose")
	await page.getByRole('presentation').getByRole('button', { name: 'Close' }).click()
	await expect(page.getByRole('presentation')).not.toBeVisible()
})