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
import path from "path"
import user from "../fixtures/admin.json"
import extension from "../fixtures/extension-admin.json"
import RegexEscape from "regex-escape";

test.beforeEach(async ({ page }) => {
	await page.goto('/')
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await page.getByRole('button', { name: 'User Info' }).click();
	await page.getByRole('link', { name: 'Admin Dashboard' }).click();
	await page.getByRole('button', { name: 'Extensions', exact: true }).click()
})

test('extension admin', async ({ page }) => {
	await test.step('get extension', async () => {
		await page.getByPlaceholder('Namespace').fill('rust-lang')
		await page.getByPlaceholder('Extension').fill('rust-analyzer')
		await page.getByRole('button', { name: 'Search Extension', exact: true }).click()
		await expect(page.getByRole('progressbar')).toBeVisible()
		await page.route('http://localhost:8080/admin/extension/rust-lang/rust-analyzer', async (route) => route.fulfill({ json: extension }))
		await expect(page.getByRole('progressbar')).not.toBeVisible()
		await expect(page.getByRole('heading', { name: extension.name, exact: true })).toBeVisible()
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
		const tokens = ['delete-extension-csrf', 'forbidden-csrf', 'delete-extension-csrf']
		await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: tokens.pop() } }))
		await page.route('http://localhost:8080/admin/extension/rust-lang/rust-analyzer/delete', async (route) => {
			const csrfToken = await route.request().headerValue('x-csrf-token')
			if (csrfToken !== 'delete-extension-csrf') {
				route.fulfill({ status: 403 })
				return
			}

			const json = route.request().postDataJSON()[0].version === '0.4.2712' ? { success: 'Extension version removed' } : { error: 'Extension not found' }
			route.fulfill({ json })
		})

		await page.getByRole('checkbox', { name: '0.4.2601', exact: true }).click()
		await expect(page.getByRole('checkbox', { name: '0.4.2601', exact: true })).toBeChecked()
		await page.getByText('Remove Versions').click()
		await expect(page.getByRole('presentation')).toBeVisible()
		await page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true }).click()
		await expect(page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true })).toBeDisabled()
		await expect(page.getByRole('presentation')).not.toBeVisible()

		const version = '0.4.2712'
		const allTargetPlatformVersions = extension.allTargetPlatformVersions.filter((value) => value.version !== version)
		const removedExtension = extension.allTargetPlatformVersions.find((value) => value.version === version)
		const newExtension = { ...extension, allTargetPlatformVersions }
		await page.getByRole('checkbox', { name: version, exact: true }).click()
		await expect(page.getByRole('checkbox', { name: version, exact: true })).toBeChecked()
		await page.getByText('Remove Versions').click()
		await expect(page.getByRole('presentation')).toBeVisible()
		await expect(page.getByRole('presentation')).toHaveText('Remove 3 versions of rust-analyzer?0.4.2712 (macOS Apple Silicon)0.4.2712 (Linux x64)0.4.2712 (Windows ARM)CancelRemove')
		await page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true }).click()
		await expect(page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true })).toBeDisabled()

		await expect(page.getByText('Request failed: POST http://localhost:8080/admin/extension/rust-lang/rust-analyzer/delete (Forbidden)')).toBeVisible()
		await page.getByRole('presentation').getByRole('button', { name: 'Close', exact: true }).click()

		await page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true }).click()
		await expect(page.getByRole('presentation').getByRole('button', { name: 'Remove', exact: true })).toBeDisabled()
		await page.route('http://localhost:8080/admin/extension/rust-lang/rust-analyzer', async (route) => route.fulfill({ json: newExtension }))
		await expect(page.getByRole('presentation')).not.toBeVisible()
		await expect(page.getByRole('progressbar')).not.toBeVisible()
		await expect(page.getByRole('heading', { name: extension.name, exact: true })).toBeVisible()
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

test('no extension found', async ({ page }) => {
	await page.getByPlaceholder('Namespace').fill('rust-lang')
	await page.getByPlaceholder('Extension').fill('rust-analyzer')
	await page.getByRole('button', { name: 'Search Extension', exact: true }).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/extension/rust-lang/rust-analyzer', async (route) => route.fulfill({ status: 404 }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByText('Extension not found: rust-lang.rust-analyzer')).toBeVisible()
})

test('not admin', async ({ page }) => {
	await page.getByPlaceholder('Namespace').fill('rust-lang')
	await page.getByPlaceholder('Extension').fill('rust-analyzer')
	await page.getByRole('button', { name: 'Search Extension', exact: true }).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/extension/rust-lang/rust-analyzer', async (route) => route.fulfill({ status: 403 }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByRole('presentation').getByText('Request failed: GET http://localhost:8080/admin/extension/rust-lang/rust-analyzer (Forbidden)')).toBeVisible()
})

test('icon', async ({ page }) => {
	await page.getByPlaceholder('Namespace').fill('rust-lang')
	await page.getByPlaceholder('Extension').fill('rust-analyzer')
	await page.getByRole('button', { name: 'Search Extension', exact: true }).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/extension/rust-lang/rust-analyzer', async (route) => route.fulfill({ json: extension }))
	await page.route('http://localhost:8080/api/rust-lang/rust-analyzer/darwin-arm64/0.4.2712/file/icon.png', async (route) => route.fulfill({ path: path.normalize(__dirname + '/../fixtures/icon128.png') }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByAltText(extension.name)).toHaveAttribute('src', new RegExp('^' + RegexEscape('blob:http://localhost:3000/')))
})

test('icon not available', async ({ page }) => {
	await page.getByPlaceholder('Namespace').fill('rust-lang')
	await page.getByPlaceholder('Extension').fill('rust-analyzer')
	await page.getByRole('button', { name: 'Search Extension', exact: true }).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/extension/rust-lang/rust-analyzer', async (route) => route.fulfill({ json: extension }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByAltText(extension.name)).not.toBeVisible()
})

test('icon not found', async ({ page }) => {
	await page.getByPlaceholder('Namespace').fill('rust-lang')
	await page.getByPlaceholder('Extension').fill('rust-analyzer')
	await page.getByRole('button', { name: 'Search Extension', exact: true }).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/extension/rust-lang/rust-analyzer', async (route) => route.fulfill({ json: extension }))
	await page.route('http://localhost:8080/api/rust-lang/rust-analyzer/darwin-arm64/0.4.2712/file/icon.png', async (route) => route.fulfill({ status: 404 }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByAltText(extension.name)).not.toBeVisible()
})

test('icon exception', async ({ page }) => {
	await page.getByPlaceholder('Namespace').fill('rust-lang')
	await page.getByPlaceholder('Extension').fill('rust-analyzer')
	await page.getByRole('button', { name: 'Search Extension', exact: true }).click()
	await expect(page.getByRole('progressbar')).toBeVisible()
	await page.route('http://localhost:8080/admin/extension/rust-lang/rust-analyzer', async (route) => route.fulfill({ json: extension }))
	await page.route('http://localhost:8080/api/rust-lang/rust-analyzer/darwin-arm64/0.4.2712/file/icon.png', async (route) => route.fulfill({ status: 400 }))
	await expect(page.getByRole('progressbar')).not.toBeVisible()
	await expect(page.getByAltText(extension.name)).not.toBeVisible()
})