/********************************************************************************
 * Copyright (c) 2019-2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as express from 'express';
import * as path from 'path';

const app = express();

// Serve static resources
const staticPath = path.join(__dirname, '..', '..', 'static');
app.use(express.static(staticPath));

// Enable react-router by forwarding the main page to all paths that don't match a resource
app.get('*', (req, res) => {
    res.sendFile(path.join(staticPath, 'index.html'));
});

const port = 3000;
app.listen(port, () => console.log(`Web UI server running on port ${port}...`));
