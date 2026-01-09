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
import json from './fixtures/namespace-details.json'
import RegexEscape from "regex-escape";

test.beforeEach(async ({page}) => {
    await page.goto('/namespace/Prisma')
})

test('namespace details', async ({page}) => {
    await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
    await page.route('http://localhost:8080/api/Prisma/details', async (route) => route.fulfill({ json }))

    const container = page.locator('.MuiGrid-container')
    for (let i = 0; i < json.extensions.length; i++) {
        const { displayName } = json.extensions[i]
        await expect(container.locator('h6').nth(i)).toHaveText(new RegExp('^' + RegexEscape(displayName)))
    }

    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
})

test('namespace details error', async ({page}) => {
    await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
    await page.route('http://localhost:8080/api/Prisma/details', async (route) => route.fulfill({ json: {error: "Namespace deactivated"} }))
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
    await expect(page.getByRole('presentation').getByText('Namespace deactivated')).toBeVisible()
})

test('namespace details not found', async ({page}) => {
    await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
    await page.route('http://localhost:8080/api/Prisma/details', async (route) => route.fulfill({ status: 404 }))
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
    await expect(page.getByText('Namespace Not Found: Prisma')).toBeVisible()
})

test('namespace details exception', async ({page}) => {
    await expect(page.locator('.MuiLinearProgress-root')).toBeVisible()
    await page.route('http://localhost:8080/api/Prisma/details', async (route) => route.fulfill({ status: 400 }))
    await expect(page.locator('.MuiLinearProgress-root')).not.toBeVisible()
    await expect(page.getByRole('presentation').getByText('Request failed: GET http://localhost:8080/api/Prisma/details (Bad Request)')).toBeVisible()
})