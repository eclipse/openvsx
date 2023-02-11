import * as fs from "fs";
import { ExtensionSignatureVerificationError } from "./errors";
import { downloadPublicKey, loadPrivateKey, loadPublicKey } from './keys';
import { signFile } from './sign';
import { SIGNED_ARCHIVE_NAME } from "./constants";
import { verifySignature } from "./verify";

/**
 * Sign an extension package. The signature is saved to `extension.sigzip`
 * @param vsixFilePath the path to the `.vsix` file of the extension
 * @param privateKeyFilePath the path to the private key used to sign the extension
 */
export const sign = async (vsixFilePath: string, privateKeyFilePath: string, options?: {
    output?: string;
}): Promise<void> => {
    const extensionFile = await fs.promises.readFile(vsixFilePath);
    const privateKey = await loadPrivateKey(privateKeyFilePath);
    const outputPath = options?.output ?? `./${SIGNED_ARCHIVE_NAME}`;

    const signature = await signFile(extensionFile, privateKey);
    await fs.promises.writeFile(outputPath, signature);

    console.info(`Signature file created at ${outputPath}`);
};

/**
* Verify an extension package against a signature archive
* @param vsixFilePath The extension file path.
* @param signatureArchiveFilePath The signature archive file path.
* @throws { ExtensionSignatureVerificationError } An error with a code indicating the validity, integrity, or trust issue
 * found during verification or a more fundamental issue (e.g.:  a required dependency was not found).
*/
export const verify = async (vsixFilePath: string, signatureArchiveFilePath: string, options?: {
    publicKey?: string;
}): Promise<boolean> => {

    if (!fs.existsSync(vsixFilePath)) {
        throw new ExtensionSignatureVerificationError(3, false);
    }

    if (!fs.existsSync(signatureArchiveFilePath)) {
        throw new ExtensionSignatureVerificationError(6, false);
    }

    const extensionFile = await fs.promises.readFile(vsixFilePath);
    const publicKey = await loadPublicKey(options?.publicKey || await downloadPublicKey());
    const signature = await fs.promises.readFile(signatureArchiveFilePath);
    const signatureValid = await verifySignature(extensionFile, publicKey, signature);

    if (!signatureValid) {
        console.error("Signature is not valid");
        throw new ExtensionSignatureVerificationError(102, true);
    }

    console.info("Signature is valid");
    return true;
};