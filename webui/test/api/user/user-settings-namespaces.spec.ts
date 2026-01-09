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

/*

/user-settings/namespaces

*/
test.beforeEach(async ({ page }) => {
    await page.goto('/user-settings')
    await page.route('http://localhost:8080/user', async (route) => route.fulfill({ json: user }))
    await page.route('http://localhost:8080/login-providers', async (route) => route.fulfill({ json: { loginProviders: { github: 'https://github.com' } } }))
    await page.getByRole('button', { name: 'User Info' }).click();
    await page.getByRole('link', { name: 'Settings' }).click();
})

test('', async ({page}) => {})


/*

src/pages/user/add-namespace-member-dialog.tsx
    service.setNamespaceMember
        props.setLoadingState
        isError
    	
    service.getUserByName
        null
        list of users

src/pages/user/user-namespace-member-list.tsx
    service.getNamespaceMembers
        handleError
    	
    service.setNamespaceMember
        props.setLoadingState
        isError
        reload members (service.getNamespaceMembers)

src/pages/user/user-settings-namespaces.tsx
    service.getNamespaces
        handleError
        loading

src/pages/user/create-namespace-dialog.tsx
    service.createNamespace
        setPosted
        isError
        props.namespaceCreated()

src/pages/user/user-namespace-details.tsx
    service.getNamespaceDetails
        loading
        isError
    	
    service.setNamespaceDetails
    service.setNamespaceLogo
        loading
        isError
        setDetailsUpdated

*/