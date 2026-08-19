# ovsx

[![Gitpod Ready-to-Code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/eclipse/openvsx/tree/master/cli)
[![NPM version](https://img.shields.io/npm/v/ovsx)](https://www.npmjs.com/package/ovsx)

Command line interface for [Eclipse Open VSX](https://open-vsx.org/). Run it via [npx](https://www.npmjs.com/package/npx) or install it with
```
npm install --global ovsx
```

`ovsx` uses open-vsx.org as default instance for publishing and downloading extensions. If you are using a different instance, specify it via the `--registryUrl` (or `-r`) argument or an environment variable named `OVSX_REGISTRY_URL`.

### Publish Extensions

You can use `ovsx` for publishing [VS Code extensions](https://code.visualstudio.com/api) to an Open VSX instance. This is very similar to [vsce](https://github.com/microsoft/vscode-vsce), the publishing tool for the [Visual Studio Code Marketplace](https://marketplace.visualstudio.com/vscode).

You must create an Open VSX [personal access token](https://open-vsx.org/user-settings/tokens) before you can use `osvx` to publish. You can either pass the token via the `--pat` (or `-p`) argument, or put it into an environment variable named `OVSX_PAT`.

Variants:
 * `ovsx publish`
   packages the extension in the current working directory using `vsce` and then publishes it.
 * `ovsx publish --packagePath <path>`
   packages the extension in the given path using `vsce` and then publishes it.
 * `ovsx publish <file>`
   publishes an already packaged file.

### Create a Namespace

The `publisher` field of your extension's package.json defines the namespace into which the extension will be published. Before you publish the first extension in a namespace, you must create it. This requires an access token as described above.

 * `ovsx create-namespace <name>`
   creates the specifed namespace. The name must correspond to the `publisher` of your extension.

Creating a namespace does _not_ automatically give you the exclusive publishing rights. Initially, everyone will be able to publish an extension with the new namespace. If you want exclusive publishing rights, you can [claim ownership of a namespace](https://github.com/eclipse/openvsx/wiki/Namespace-Access).

### Download Extensions

You can use `ovsx` for downloading extensions from an Open VSX instance. Extensions are identified with the format `namespace.extension`, and an exact version or version range can be specified with the `--versionRange` (or `-v`) argument. The namespace corresponds to the `publisher` entry of the package.json file.

Variants:
 * `ovsx get <extension>`
   downloads an extension and saves it to a file as specified in its download URL in the current working directory. This is usually in the format `namespace.extension-version.vsix`. For [target platform specific extensions](https://code.visualstudio.com/api/working-with-extensions/publishing-extension#platformspecific-extensions) (e.g. `linux-x64`) the format is `namespace.extension-version@target.vsix`.
 * `ovsx get <extension> -o <path>`
   downloads an extension and saves it in the specified file or directory.
 * `ovsx get <extension> --metadata`
   downloads the JSON metadata of an extension and prints it to the standard output.
 * `ovsx get <extension> --metadata -o <path>`
   downloads the JSON metadata of an extension and saves it in the specified file or directory.

### Store Access Tokens

The `login` command lets you store an access token for a namespace.

 * `ovsx login <name>`
   the name must correspond to the `publisher` of your extension. `ovsx` will ask you to provide an access token.

The `logout` command lets you remove a stored access token.

 * `ovsx logout <name>`
   the name must correspond to the `publisher` of your extension.

By default `ovsx` stores access tokens in the operating system's credential manager (via [`cross-keychain`](https://www.npmjs.com/package/cross-keychain)), falling back to storing them as plaintext in the `~/.ovsx` file if the credential manager can't be used. You can also set the environment variable `OVSX_STORE=file` to force plaintext storage; this is strongly discouraged, as it leaves your tokens readable by anyone with access to your home directory.

### Programmatic API

Every command is available as a function, so `ovsx` can be scripted instead of shelled out to. This is
useful for publishing to Open VSX and the Visual Studio Marketplace from a single script:

```ts
import { createVSIX } from '@vscode/vsce';
import { publishVSIX } from 'ovsx';

const vsix = 'my-extension-1.0.0.vsix';
await createVSIX({ packagePath: vsix });
await publishVSIX(vsix, { pat: process.env.OVSX_PAT });
```

`publishVSIX` publishes packages that exist already and resolves with the metadata of the published
extensions, rejecting as soon as one of them fails. `createVSIX` packages an extension — delegating to
`vsce` and validating the license if the target registry requires one — and resolves with the path of
the package it wrote.

Unlike the command line interface, these two functions never ask for input: a missing access token is
an error rather than a prompt. Pass `interactive: true` to opt back into prompting.

Progress messages go to the console by default. Pass a `log` implementation to capture or silence
them (note that this covers the output of `ovsx` itself; `vsce` reports the progress of packaging on
its own):

```ts
import { publishVSIX, silentLogger } from 'ovsx';

await publishVSIX(vsix, { pat: process.env.OVSX_PAT, log: silentLogger });
```

The lower level building blocks are exported as well: `publish` (packages and publishes, reporting the
outcome of every package and target separately), `getExtension`, `createNamespace`, `verifyPat` and the
`Registry` class that wraps the registry API.
