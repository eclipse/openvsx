## Eclipse Open VSX Change Log

This change log covers only the command line interface (CLI) of Open VSX.

### v0.3.0 (Jan. 2022)

#### Breaking Changes

- The minimum version of Node.js required is now `14` because of the newer `vsce`

#### Dependencies

- Upgrade `vsce` from `1.97.0` to `2.6.3` ([#403](https://github.com/eclipse/openvsx/pull/403))
- Upgrade `follow-redirects`
- Upgrade `nth-check`

---

### v0.2.1 (Sep. 2021)

#### New Features

- Also accept `LICENCE` files (alternative spelling to `LICENSE`) ([#307](https://github.com/eclipse/openvsx/pull/307))
- Remove `--web` option (it will now be inferred from the `package.json` file)

#### Dependencies

- Upgrade `vsce` from `1.93.0` to `1.97.0`

---

### v0.2.0 (Jun. 2021)

#### New Features

- Added CLI parameter `--web` for web extensions ([#262](https://github.com/eclipse/openvsx/pull/262))

#### Bug Fixes

- Fixed handling of unknown commands ([#302](https://github.com/eclipse/openvsx/issues/302))

#### Dependencies

- Updated the `vsce` dependency from 1.84.0 to 1.93.0 ([#300](https://github.com/eclipse/openvsx/issues/300))
- Added explicit dependency to `tmp` ([#254](https://github.com/eclipse/openvsx/issues/254))

---

### v0.1.0 (Apr. 2021)

First release of Open VSX with the Eclipse Foundation.
