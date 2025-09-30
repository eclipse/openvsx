#!/usr/bin/env node

const semver = require('semver');

if (semver.lt(process.versions.node, '20.0.0')) {
    console.error('ovsx requires at least NodeJS version 20. Check your installed version with `node --version`.');
    process.exit(1);
}

require('./lib/main')(process.argv);
