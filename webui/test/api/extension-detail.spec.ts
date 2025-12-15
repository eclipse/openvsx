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
import response from "./fixtures/pyrefly/extension-pyrefly.json"
import oldReviews from "./fixtures/pyrefly/reviews-old.json"
import newReviews from "./fixtures/pyrefly/reviews-new.json"
import user from "./fixtures/user.json"
import RegexEscape from "regex-escape";

test.beforeEach(async ({ page }) => {
	await page.goto('/extension/meta/pyrefly')
})

test('extension detail', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
})

test('icon', async ({ page }) => {
	const json = JSON.parse(JSON.stringify(response))
	json.files.icon = "http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/pyrefly-symbol.png"

	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json }))
	await page.route('http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/pyrefly-symbol.png', async (route) => route.fulfill({ path: path.normalize(__dirname + '/fixtures/pyrefly/pyrefly-symbol.png') }))
	await expect(page.getByAltText('Pyrefly - Python Language Tooling')).toHaveAttribute('src', new RegExp('^' + RegexEscape('blob:http://localhost:3000/')))
})

test('icon not available', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByAltText('Pyrefly - Python Language Tooling')).toHaveAttribute('src', '/default-icon.png')
})

test('icon not found', async ({ page }) => {
	const json = JSON.parse(JSON.stringify(response))
	json.files.icon = "http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/pyrefly-symbol.png"

	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json }))
	await page.route('http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/pyrefly-symbol.png', async (route) => route.fulfill({ status: 404 }))
	await expect(page.getByText("Extension Not Found: meta.pyrefly")).toBeVisible()
})

test('icon exception', async ({ page }) => {
	const json = JSON.parse(JSON.stringify(response))
	json.files.icon = "http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/pyrefly-symbol.png"

	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json }))
	await page.route('http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/pyrefly-symbol.png', async (route) => route.fulfill({ status: 400 }))
	await expect(page.getByRole('presentation')).toHaveText(new RegExp(RegexEscape('Request failed: GET http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/pyrefly-symbol.png (Bad Request)')))
})

test('readme not available', async ({ page }) => {
	const json = JSON.parse(JSON.stringify(response))
	json.files.readme = undefined

	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await expect(page.getByText('No README available')).toBeVisible()
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('readme', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/README.md', async (route) => route.fulfill({ path: path.normalize(__dirname + '/fixtures/pyrefly/README.md') }))
	await expect(page.getByText('Pyrefly VS Code Extension')).toBeVisible()
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('readme exception', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/README.md', async (route) => route.fulfill({ status: 404 }))
	await expect(page.getByRole('presentation')).toHaveText(new RegExp(RegexEscape('Request failed: GET http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/README.md (Not Found)')))
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('changelog not available', async ({ page }) => {
	const json = JSON.parse(JSON.stringify(response))
	json.files.changelog = undefined

	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.getByText('Changes').click()
	await expect(page.getByText('No changelog available')).toBeVisible()
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('changelog', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.getByText('Changes').click()
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/changelog.md', async (route) => route.fulfill({ path: path.normalize(__dirname + '/fixtures/pyrefly/changelog.md') }))
	await expect(page.getByText('All notable changes to this project will be documented in this file.')).toBeVisible()
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('changelog exception', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.getByText('Changes').click()
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/changelog.md', async (route) => route.fulfill({ status: 404 }))
	await expect(page.getByRole('presentation')).toHaveText(new RegExp(RegexEscape('Request failed: GET http://localhost:8080/api/meta/pyrefly/alpine-arm64/0.46.0/file/changelog.md (Not Found)')))
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('reviews', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.getByText('Ratings & Reviews').click()
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/reviews', async (route) => route.fulfill({ json: oldReviews }))
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
	await expect(page.getByText(/\d+ years agotheArianit/)).toBeVisible()
})

test('reviews exception', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.getByText('Ratings & Reviews').click()
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/reviews', async (route) => route.fulfill({ status: 400 }))
	await expect(page.getByRole('presentation')).toHaveText(new RegExp(RegexEscape('Request failed: GET http://localhost:8080/api/meta/pyrefly/reviews (Bad Request)')))
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('post review', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await expect(page.getByText('Publish')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.getByText('Ratings & Reviews').click()
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/reviews', async (route) => route.fulfill({ json: oldReviews }))
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'post-review-csrf' } }))
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
	await expect(page.getByText('Write a review')).toBeVisible()
	await page.getByText('Write a review').click()
	await page.getByLabel('Your review...').fill('Test')
	await page.getByText('Post review').click()
	await page.route('http://localhost:8080/api/eamodio/gitlens/review', async (route) => {
		const csrfToken = await route.request().headerValue('x-csrf-token')
		if (csrfToken !== 'post-review-csrf') {
			route.fulfill({ status: 403 })
			return
		}

		const json = route.request().postDataJSON().comment === 'Test' ? { success: 'Review added' } : { error: 'Review rejected' }
		route.fulfill({ json })
	})
	await expect(page.getByText('Post review')).toBeDisabled()
	await page.route('http://localhost:8080/api/meta/pyrefly/reviews', async (route) => route.fulfill({ json: newReviews }))
	await expect(page.getByText('Write a review')).not.toBeVisible()
	await expect(page.getByText('Revoke my review')).toBeVisible()
	await expect(page.getByText(/\d+ (months|years?) agoamvanbaren/)).toBeVisible()
})

test('post duplicate review', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await expect(page.getByText('Publish')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.getByText('Ratings & Reviews').click()
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/reviews', async (route) => route.fulfill({ json: oldReviews }))
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'post-review-csrf' } }))
	await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
	await expect(page.getByText('Write a review')).toBeVisible()
	await page.getByText('Write a review').click()
	await page.getByLabel('Your review...').fill('Test')
	await page.getByText('Post review').click()
	await page.route('http://localhost:8080/api/eamodio/gitlens/review', async (route) => {
		route.fulfill({ json: { error: 'Review already posted' } })
	})
	await expect(page.getByRole('presentation').getByText('Review already posted')).toBeVisible()
	await page.getByRole('button', { name: 'Close' }).click()
	await expect(page.getByText('Post review')).toBeDisabled()
})

test('delete review', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await expect(page.getByText('Publish')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.getByText('Ratings & Reviews').click()
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/reviews', async (route) => route.fulfill({ json: newReviews }))
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'delete-review-csrf' } }))
	await page.getByText('Revoke my review').click()
	await page.route('http://localhost:8080/api/eamodio/gitlens/review/delete', async (route) => {
		const csrfToken = await route.request().headerValue('x-csrf-token')
		if (csrfToken !== 'delete-review-csrf') {
			route.fulfill({ status: 403 })
			return
		}

		route.fulfill({ json: { success: 'Review deleted' } })
	})
	await expect(page.getByText('Revoke my review')).toBeDisabled()
	await page.route('http://localhost:8080/api/meta/pyrefly/reviews', async (route) => route.fulfill({ json: oldReviews }))
	await expect(page.getByText('Revoke my review')).not.toBeVisible()
	await expect(page.getByText('Write a review')).toBeVisible()
	await expect(page.getByText(/\d+ (months|years?) agoamvanbaren/)).not.toBeVisible()
})

test('delete review error', async ({ page }) => {
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
	await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
	await expect(page.getByText('Publish')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly', async (route) => route.fulfill({ json: response }))
	await expect(page.getByText('Pyrefly - Python Language Tooling')).toBeVisible()
	await page.getByText('Ratings & Reviews').click()
	await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
	await page.route('http://localhost:8080/api/meta/pyrefly/reviews', async (route) => route.fulfill({ json: newReviews }))
	await page.route('http://localhost:8080/user/csrf', async (route) => route.fulfill({ json: { header: 'x-csrf-token', value: 'delete-review-csrf' } }))
	await page.getByText('Revoke my review').click()
	await page.route('http://localhost:8080/api/eamodio/gitlens/review/delete', async (route) => {
		const csrfToken = await route.request().headerValue('x-csrf-token')
		if (csrfToken !== 'delete-review-csrf') {
			route.fulfill({ status: 403 })
			return
		}

		route.fulfill({ json: { error: 'Review not found' } })
	})
	await expect(page.getByRole('presentation').getByText('Review not found')).toBeVisible()
	await page.getByRole('button', { name: 'Close' }).click()
	await expect(page.getByText('Revoke my review')).toBeDisabled()
})