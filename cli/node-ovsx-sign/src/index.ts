import * as commander from "commander";
import * as fs from "fs";
import * as crypto from "crypto";
import { execSync } from "child_process";
import { downloadPublicKey } from "./lib/downloadPublicKey";

const SIGNED_ARCHIVE_NAME = "extension.sigzip";
const HASHED_PACKAGE_NAME = "extension.vsix.hash";

module.exports = function (argv: string[]): void {
  const program = new commander.Command();
  program.usage("<command> [options]");

  const verifyCmd = program.command("verify");
  verifyCmd
    .description("Verify an extension package")
    .arguments("<extensionpackage> <signaturearchive>")
    .action(async (extensionPackage, signatureArchive) => {
      const extensionFile = await fs.promises.readFile(extensionPackage);

      const publicKeyLocation = await downloadPublicKey();

      const hashOfExtensionFile = crypto.createHash("sha256").update(extensionFile).digest("hex");
      await fs.promises.writeFile(`./${HASHED_PACKAGE_NAME}`, hashOfExtensionFile);

      const signatureHash = execSync(`openssl pkeyutl -verify -pubin -inkey ${publicKeyLocation} -sigfile ${signatureArchive} -in ./${HASHED_PACKAGE_NAME}`);

      // Cleanup
      await fs.promises.unlink(`./${HASHED_PACKAGE_NAME}`);

      console.info(signatureHash.toString("utf8"));
    });

  const signCmd = program.command("sign");
  signCmd
    .description("Sign an extension package")
    .arguments("<extensionpackage> <privatekey>")
    .action(async (extensionPackage, privateKey) => {
      const extensionFile = await fs.promises.readFile(extensionPackage);

      const extensionPackageHash = crypto.createHash("sha256").update(extensionFile).digest("hex");
      await fs.promises.writeFile(`./${HASHED_PACKAGE_NAME}`, extensionPackageHash);

      const signature = execSync(`openssl pkeyutl -sign -inkey ${privateKey} -in ./${HASHED_PACKAGE_NAME}`);

      // Cleanup
      await fs.promises.unlink(`./${HASHED_PACKAGE_NAME}`);

      await fs.promises.writeFile(`./${SIGNED_ARCHIVE_NAME}`, signature);

      console.info(`Signature file created at ./${SIGNED_ARCHIVE_NAME}`);
    });

  program.parse(argv);

  if (process.argv.length <= 2) {
    program.help();
  }
};

