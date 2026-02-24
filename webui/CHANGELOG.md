# Eclipse Open VSX Frontend Library Change Log

This change log covers only the frontend library (webui) of Open VSX.

## [v0.19.0] (unreleased)

### Changed

- Switch from webpack to vite for building the bundle ([#1399](https://github.com/eclipse/openvsx/pull/1399))

### Dependencies

- Bump @isaacs/brace-expansion from `5.0.0` to `5.0.1` ([#1638](https://github.com/eclipse/openvsx/pull/1638))

## [v0.18.0] (Feb. 2026)

### Added

- Support removing reviews by admins ([#1403](https://github.com/eclipse/openvsx/pull/1403))
- Support for GitHub flavored markdown alerts ([#1535](https://github.com/eclipse/openvsx/pull/1535))
- Support customizing the publisher agreement name and contact email ([#1550](https://github.com/eclipse/openvsx/pull/1550))
- Display the unique identifier on the extension details page ([#1590](https://github.com/eclipse/openvsx/pull/1590))
- Add admin pages to edit / view data for dynamic rate limits ([#1569](https://github.com/eclipse/openvsx/pull/1569))
- Add ability to browse admin logs via the admin dashboard ([#1582](https://github.com/eclipse/openvsx/pull/1582))

### Changed

- Disable the automatic execution of lifecycle scripts by yarn ([#1546](https://github.com/eclipse/openvsx/pull/1546))

### Dependencies

- Upgrade `lodash` from `4.17.21` to `4.17.23` ([#1557](https://github.com/eclipse/openvsx/pull/1557))
- Upgrade `react-router` from `6.23.1` to `6.30.3` ([#1532](https://github.com/eclipse/openvsx/pull/1532))
- Upgrade `react-router-dom` from `6.23.1` to `6.30.3`
- Upgrade `@playwright/test` from `1.57.0` to `1.58.0`

## [v0.17.1] (Jan. 2026)

### Dependencies

- Upgrade `rimraf` from `6.0.1` to `6.1.2`
- Upgrade `@playwright/test` from `1.49.0` to `1.55.1`
- Upgrade `mocha` from `10.8.2` to `11.7.5`
- Upgrade `ts-mocha` from `10.0.0` to `11.1.0`
- Add `ts-node` version `10.9.2`
