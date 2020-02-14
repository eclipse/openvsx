# ovsx

Command line interface for [Eclipse Open VSX Registry](https://open-vsx.org/).

`ovsx` uses open-vsx.org as default instance for publishing and downloading extensions. If you are using a different instance, specify it via the `--registryUrl` (or `-r`) argument or an environment variable named `OVSX_REGISTRY_URL`.

### Publish Extensions

You can use `ovsx` for publishing [VS Code extensions](https://code.visualstudio.com/api) to an Open VSX instance. This is very similar to [vsce](https://github.com/microsoft/vscode-vsce), the publishing tool for the [Visual Studio Code Marketplace](https://marketplace.visualstudio.com/vscode).

You must create a personal access token before you can use `osvx` to publish. You can either pass the token via the `--pat` (or `-p`) argument, or put it into an environment variable named `OVSX_PAT`.

Variants:
 * `ovsx publish`
   packages the extension in the current working directory using `vsce` and then publishes it.
 * `ovsx publish --packagePath <path>`
   packages the extension in the given path using `vsce` and then publishes it.
 * `ovsx publish <file>`
   publishes an already packaged file.

### Download Extensions

You can use `ovsx` for downloading extensions from an Open VSX instance. Extensions are identified with the format `publisher.extension`, and an exact version or version range can be specified with the `--version` (or `-v`) argument.

Variants:
 * `ovsx get <extension>`
   downloads an extension and saves it in a file as specified in its download URL (usually in the format `publisher.extension-version.vsix`) in the current working directory.
 * `ovsx get <extension> -o <path>`
   downloads an extension and saves it in the specified file or directory.
 * `ovsx get <extension> --metadata`
   downloads the JSON metadata of an extension and prints it to the standard output.
 * `ovsx get <extension> --metadata -o <path>`
   downloads the JSON metadata of an extension and saves it in the specified file or directory.
