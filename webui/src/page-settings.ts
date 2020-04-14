/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { Extension } from "./extension-registry-types";

export interface PageSettings {
    pageTitle: string;
    toolbarText?: string;
    listHeaderTitle?: string;
    logoURL?: string;
    logoAlt?: string;
    extensionDefaultIconURL?: string;
    namespaceAccessInfoURL?: string;
    reportAbuseHref?: (extension: Extension) => string;
    claimNamespaceHref?: (string: string) => string;
}
