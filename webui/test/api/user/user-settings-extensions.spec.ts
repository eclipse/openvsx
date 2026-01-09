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
import extensions from "../fixtures/user-settings/user-settings-extensions.json"
import extension from "../fixtures/user-settings/user-settings-delete-extension.json"

/*

/user-settings/extensions
/user-settings/extensions/:namespace/:extension/delete

*/
test.beforeEach(async ({ page }) => {
	await page.goto('/')
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await page.getByRole('button', { name: 'User Info' }).click();
	await page.getByRole('link', { name: 'Settings' }).click();
	await page.getByText('Extensions').click()
})

test('extensions forbidden', async ({ page }) => {
	const progressbar = page.getByRole('progressbar').first();
	await expect(progressbar).toBeVisible();
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'extensions-csrf' } }))
	await page.route('http://localhost:8080/user/extensions', async (route) => route.fulfill({ status: 403 }))
	await expect(progressbar).not.toBeVisible()
	await expect(page.getByText('Request failed: GET http://localhost:8080/user/extensions (Forbidden)', { exact: true })).toBeVisible()
})

test('get extensions', async ({ page }) => {
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'extensions-csrf' } }))
	await page.route('http://localhost:8080/user/extensions', async (route) => route.fulfill({ json: extensions }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText('rust-analyzer', { exact: true })).toBeVisible()
	await expect(page.getByText('XML Tools', { exact: true })).toBeVisible()
	await expect(page.getByText('Vim', { exact: true })).toBeVisible()
})

test('delete extension not found', async ({ page }) => {
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'extensions-csrf' } }))
	await page.route('http://localhost:8080/user/extensions', async (route) => route.fulfill({ json: extensions }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await page.getByTestId('DeleteIcon').nth(1).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/extension/DotJoshJohnson/xml', async (route) => route.fulfill({ status: 404 }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page).toHaveURL('/user-settings/extensions')
})

test('delete extension forbidden', async ({ page }) => {
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'extensions-csrf' } }))
	await page.route('http://localhost:8080/user/extensions', async (route) => route.fulfill({ json: extensions }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await page.getByTestId('DeleteIcon').nth(1).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/user/extension/DotJoshJohnson/xml', async (route) => route.fulfill({ status: 403 }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText('Request failed: GET http://localhost:8080/user/extension/DotJoshJohnson/xml (Forbidden)', { exact: true })).toBeVisible()
})

test('user remove extension', async ({ page }) => {
	let tokenIndex = 0
	const tokens = ['extensions-csrf', 'delete-extension-csrf', 'forbidden-csrf', 'delete-extension-csrf']
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: tokens[tokenIndex] } }))
	await test.step('get extension', async () => {
		await expect(page.getByRole('progressbar')).toBeVisible()
		await page.route('http://localhost:8080/user/extensions', async (route) => route.fulfill({ json: extensions }))
		await expect(page.getByRole('progressbar')).not.toBeVisible()
		await page.getByTestId('DeleteIcon').nth(1).click()
		await expect(page.getByRole('progressbar')).toBeVisible()
		await page.route('http://localhost:8080/user/extension/DotJoshJohnson/xml', async (route) => route.fulfill({ json: extension }))
		await expect(page.getByRole('progressbar')).not.toBeVisible()
		await expect(page.getByRole('heading', { name: extension.displayName, exact: true })).toBeVisible()
		await expect(page.getByText(extension.description)).toBeVisible()
		for (const targetPlatformVersion of extension.allTargetPlatformVersions) {
			const version = targetPlatformVersion.version
			await expect(page.getByRole('checkbox', { name: version, exact: true })).toBeVisible()
			for (const targetPlatform of targetPlatformVersion.targetPlatforms) {
				await expect(page.locator(`input[name="${targetPlatform}/${version}"]`)).toBeVisible()
			}
		}
	})
	await test.step('remove extension', async () => {
		await page.route('http://localhost:8080/user/extension/DotJoshJohnson/xml/delete', async (route) => {
			const csrfToken = await route.request().headerValue('x-csrf-token')
			if (csrfToken !== 'delete-extension-csrf') {
				route.fulfill({ status: 403 })
				return
			}

			const json = route.request().postDataJSON()[0].version === '2.5.0' ? { success: 'Extension version removed' } : { error: 'Extension not found' }
			route.fulfill({ json })
		})

		await page.getByRole('checkbox', { name: '2.4.1', exact: true }).click()
		await expect(page.getByRole('checkbox', { name: '2.4.1', exact: true })).toBeChecked()
		await page.getByText('Remove Versions').click()
		await expect(page.getByRole('presentation')).toBeVisible()
		tokenIndex++
		await page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true }).click()
		await expect(page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true })).toBeDisabled()
		await expect(page.getByRole('presentation')).not.toBeVisible()

		const version = '2.5.0'
		const allTargetPlatformVersions = extension.allTargetPlatformVersions.filter((value) => value.version !== version)
		const removedExtension = extension.allTargetPlatformVersions.find((value) => value.version === version)
		const newExtension = { ...extension, allTargetPlatformVersions }
		await page.getByRole('checkbox', { name: version, exact: true }).click()
		await expect(page.getByRole('checkbox', { name: version, exact: true })).toBeChecked()
		await page.getByText('Remove Versions').click()
		await expect(page.getByRole('presentation')).toBeVisible()
		await expect(page.getByRole('presentation')).toHaveText('Remove 3 versions of xml?2.5.0 (Universal)2.5.0 (Linux x64)2.5.0 (Windows ARM)CancelRemove')
		tokenIndex++
		await page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true }).click()
		await expect(page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true })).toBeDisabled()

		await expect(page.getByText('Request failed: POST http://localhost:8080/user/extension/DotJoshJohnson/xml/delete (Forbidden)')).toBeVisible()
		await page.getByRole('presentation').getByRole('button', { name: 'Close', exact: true }).click()

		tokenIndex++
		await page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true }).click()
		await expect(page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true })).toBeDisabled()
		await page.route('http://localhost:8080/user/extension/DotJoshJohnson/xml', async (route) => route.fulfill({ json: newExtension }))
		await expect(page.getByRole('presentation')).not.toBeVisible()
		await expect(page.getByRole('progressbar')).not.toBeVisible()
		await expect(page.getByRole('heading', { name: extension.displayName, exact: true })).toBeVisible()
		await expect(page.getByText(extension.description)).toBeVisible()
		for (const targetPlatformVersion of newExtension.allTargetPlatformVersions) {
			const version = targetPlatformVersion.version
			await expect(page.getByRole('checkbox', { name: version, exact: true })).toBeVisible()
			for (const targetPlatform of targetPlatformVersion.targetPlatforms) {
				await expect(page.locator(`input[name="${targetPlatform}/${version}"]`)).toBeVisible()
			}
		}

		if (removedExtension == null) {
			test.fail()
			return
		}
		await expect(page.getByRole('checkbox', { name: removedExtension.version, exact: true })).not.toBeVisible()
		for (const targetPlatform of removedExtension.targetPlatforms) {
			await expect(page.locator(`input[name="${targetPlatform}/${removedExtension.version}"]`)).not.toBeVisible()
		}
	})
})

/*

src/pages/user/publish-extension-dialog.tsx
	service.publishExtension
		isError
			unknown publisher
			other error
			
	service.createNamespace
		isError

src/pages/user/user-namespace-extension-list-item.tsx
	service.getExtensionIcon
		extension change

src/pages/user/user-namespace-extension-list.tsx
	service.getExtensionDetail
		loading
		isError



*/