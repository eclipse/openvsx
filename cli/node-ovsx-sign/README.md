# `node-ovsx-sign`

This package is an open-source alternative to Microsoft's proprietary `node-vsce-sign`, which enables a feature in VS Code called [**Repository signing**](https://code.visualstudio.com/updates/v1_75#_vs-marketplace-extension-signing). 

## Example usage of signing [CLI]

> **Note**: this requires access to the server-side private key for signing.

```sh
node-ovsx-sign sign extension.vsix keys/private.pem
```

## Example usage of verifying [Node module]

```ts
import { verify, ExtensionSignatureVerificationError } from "node-ovsx-sign";

(async () => {
  try {
    await verify("ext.vsix", "ext.sigzip");
    console.log("Verified successfully");
  } catch (e) {
    if (e instanceof ExtensionSignatureVerificationError) {
      console.error("Could not verify extension signature");
    } else {
      throw e;
    }
  }
})();
```