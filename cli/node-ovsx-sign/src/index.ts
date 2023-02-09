import * as commander from "commander";
import * as fs from "fs";
import * as crypto from "crypto";
import { exec } from "./utils/exec";
import { downloadPublicKey } from "./utils/downloadPublicKey";

const SIGNED_ARCHIVE_NAME = "extension.sigzip";
const HASHED_PACKAGE_NAME = "extension.vsix.hash";

/**
 * Verify an extension package against a signature archive
 * @param extensionPackage the path to the `.vsix` file of the extension
 * @param signatureArchive a `.sigzip` file containing the signature of the extension
 */
export const verify = async (extensionPackage: string, signatureArchive: string): Promise<boolean> => {
  const extensionFile = await fs.promises.readFile(extensionPackage);
  let didSucceed = true;
  const publicKeyLocation = await downloadPublicKey();

  const hashOfExtensionFile = crypto.createHash("sha256").update(extensionFile).digest("hex");
  await fs.promises.writeFile(`./${HASHED_PACKAGE_NAME}`, hashOfExtensionFile);

  const signatureHash = await exec(`openssl pkeyutl -verify -pubin -inkey ${publicKeyLocation} -sigfile ${signatureArchive} -in ./${HASHED_PACKAGE_NAME}`, {}).catch((err) => {
    console.error(err);
    didSucceed = false;
  });

  // Cleanup
  await fs.promises.unlink(`./${HASHED_PACKAGE_NAME}`);

  console.info(signatureHash);

  return didSucceed;
};

/**
 * Sign an extension package. The signature is saved to `extension.sigzip`
 * @param extensionPackage the path to the `.vsix` file of the extension
 * @param privateKey the path to the private key used to sign the extension
 */
export const sign = async (extensionPackage: string, privateKey: string): Promise<void> => {
  const extensionFile = await fs.promises.readFile(extensionPackage);

  const extensionPackageHash = crypto.createHash("sha256").update(extensionFile).digest("hex");
  await fs.promises.writeFile(`./${HASHED_PACKAGE_NAME}`, extensionPackageHash);

  const signature = await exec(`openssl pkeyutl -sign -inkey ${privateKey} -in ./${HASHED_PACKAGE_NAME}`, {});

  // Cleanup
  await fs.promises.unlink(`./${HASHED_PACKAGE_NAME}`);

  await fs.promises.writeFile(`./${SIGNED_ARCHIVE_NAME}`, signature);

  console.info(`Signature file created at ./${SIGNED_ARCHIVE_NAME}`);
};

export default function (argv: string[]): void {
  const program = new commander.Command();
  program.usage("<command> [options]");

  const verifyCmd = program.command("verify");
  verifyCmd
    .description("Verify an extension package")
    .arguments("<extensionpackage> <signaturearchive>")
    .action((extensionPackage: string, signatureArchive: string) => { verify(extensionPackage, signatureArchive) });

  const signCmd = program.command("sign");
  signCmd
    .description("Sign an extension package")
    .arguments("<extensionpackage> <privatekey>")
    .action(sign);

  program.parse(argv);

  if (process.argv.length <= 2) {
    program.help();
  }
};

