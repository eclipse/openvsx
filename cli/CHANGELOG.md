## Eclipse Open VSX Change Log

This change log covers only the command line interface (CLI) of Open VSX.

### v0.8.3 (Aug. 2023)

#### Dependencies

- Upgrade `yarn` from `1.22.19` to `3.6.1` ([#793](https://github.com/eclipse/openvsx/pull/793))
- Upgrade `word-wrap` from `1.2.3` to `1.2.4` ([#787](https://github.com/eclipse/openvsx/pull/787))

---

### v0.8.2 (July 2023)

#### Dependencies

- Upgrade `vcse` from `2.15.0` to `2.19.0` ([#775](https://github.com/eclipse/openvsx/pull/775))
- Upgrade `semver` from `5.7.1` to `7.5.2` ([#763](https://github.com/eclipse/openvsx/pull/763))

---

### v0.8.1 (May. 2023)

#### Dependencies

- Added explicit dependency to `semver` ([#733](https://github.com/eclipse/openvsx/pull/733))

---

### v0.8.0 (Jan. 2023)

#### New Features

- Added CLI parameter `--skip-duplicate` to  fail silently if version already exists on the marketplace ([#646](https://github.com/eclipse/openvsx/pull/646))

---
 
### v0.7.1 (Dec. 2022)

#### Dependencies

- Migrated from deprecated `vcse` to `@vscode/vsce` ([#637](https://github.com/eclipse/openvsx/pull/637))

---

### v0.7.0 (Dec. 2022)

#### New Features

- Added CLI parameter `--no-dependencies` to disable dependency detection ([#635](https://github.com/eclipse/openvsx/pull/635))

#### Dependencies

- Upgrade `vcse` from `2.7.0` to `2.15.0` ([#635](https://github.com/eclipse/openvsx/pull/635))

---

### v0.6.0 (Nov. 2022)

#### New Features

- Added verify-pat command ([#624](https://github.com/eclipse/openvsx/pull/624))

#### Dependencies

- Upgrade `vcse` from `2.6.3` to `2.7.0` ([#621](https://github.com/eclipse/openvsx/pull/621))

---

### v0.5.0 (Mar. 2022)

#### New Features

- Added CLI parameter `--target` to support target platforms ([#406](https://github.com/eclipse/openvsx/pull/406))

---

### v0.4.0 (Feb. 2022)

#### New Features

- Added CLI parameter `--pre-release` to support pre-releases ([#410](https://github.com/eclipse/openvsx/pull/410))

---

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
