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
