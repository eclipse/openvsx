import * as commander from "commander";
import * as fs from "fs";
import * as crypto from "crypto";
import { exec } from "./utils/exec";
import { downloadPublicKey } from "./utils/downloadPublicKey";

const SIGNED_ARCHIVE_NAME = "extension.sigzip";
const HASHED_PACKAGE_NAME = "extension.vsix.hash";

export class ExtensionSignatureVerificationError extends Error {
  code: number;
  didExecute: boolean;
  constructor(code: number, didExecute: boolean) {
    super();
    this.code = code;
    this.didExecute = didExecute;
  }
}

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
  const publicKeyLocation = options?.publicKey || await downloadPublicKey();

  const hashOfExtensionFile = crypto.createHash("sha256").update(extensionFile).digest("hex");
  await fs.promises.writeFile(`./${HASHED_PACKAGE_NAME}`, hashOfExtensionFile);

  await exec(`openssl pkeyutl -verify -pubin -inkey ${publicKeyLocation} -sigfile ${signatureArchiveFilePath} -in ./${HASHED_PACKAGE_NAME}`, {}).catch((err) => {
    console.error(err);
    if (err.message.includes("Can't open signature file")) {
      throw new ExtensionSignatureVerificationError(7, true);
    } else if (err.message.includes("Signature Verification Failure")) {
      throw new ExtensionSignatureVerificationError(102, true);
    }
    throw new ExtensionSignatureVerificationError(110, true);
  });

  // Cleanup
  await fs.promises.unlink(`./${HASHED_PACKAGE_NAME}`);

  return true;
};

/**
 * Sign an extension package. The signature is saved to `extension.sigzip`
 * @param vsixFilePath the path to the `.vsix` file of the extension
 * @param privateKeyFilePath the path to the private key used to sign the extension
 */
export const sign = async (vsixFilePath: string, privateKeyFilePath: string): Promise<void> => {
  const extensionFile = await fs.promises.readFile(vsixFilePath);

  const extensionPackageHash = crypto.createHash("sha256").update(extensionFile).digest("hex");
  await fs.promises.writeFile(`./${HASHED_PACKAGE_NAME}`, extensionPackageHash);

  await exec(`openssl pkeyutl -sign -inkey ${privateKeyFilePath} -in ./${HASHED_PACKAGE_NAME} -out ${SIGNED_ARCHIVE_NAME}`, {});

  // Cleanup
  await fs.promises.unlink(`./${HASHED_PACKAGE_NAME}`);

  console.info(`Signature file created at ./${SIGNED_ARCHIVE_NAME}`);
};

export default function (argv: string[]): void {
  const program = new commander.Command();
  program.usage("<command> [options]");

  const verifyCmd = program.command("verify");
  verifyCmd
    .description("Verify an extension package")
    .arguments("<extension-package> <signature-archive>")
    .option("-p, --public-key <public-key>", "The path to the public key to use for verification")
    .action((vsixFilePath: string, signatureArchiveFilePath: string, { publicKey }) => {
      verify(vsixFilePath, signatureArchiveFilePath, { publicKey });
    });

  const signCmd = program.command("sign");
  signCmd
    .description("Sign an extension package")
    .arguments("<extension-package> <private-key>")
    .action(sign);

  program.parse(argv);

  if (process.argv.length <= 2) {
    program.help();
  }
}

