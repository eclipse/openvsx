import * as commander from "commander";
import { verify, sign, ExtensionSignatureVerificationError } from "./utils/sign";

export default function (argv: string[]): void {
  const program = new commander.Command();
  program.usage("<command> [options]");

  const verifyCmd = program.command("verify");
  verifyCmd
    .description("Verify an extension package")
    .arguments("<extension-package> <signature-archive>")
    .option("-p, --public-key <public-key>", "The path to the public key to use for verification")
    .action(async (vsixFilePath: string, signatureArchiveFilePath: string, { publicKey }) => {
      try {
        await verify(vsixFilePath, signatureArchiveFilePath, { publicKey });
      } catch (e) {
        if (e instanceof ExtensionSignatureVerificationError) {
          process.exit(e.code);
        } else {
          console.error(e.message);
          process.exit(1);
        }
      }
    });

  const signCmd = program.command("sign");
  signCmd
    .description("Sign an extension package")
    .arguments("<extension-package> <private-key>")
    .option("-o, --output <output>", "The path to the output signature archive")
    .action(sign);

  program.parse(argv);

  if (process.argv.length <= 2) {
    program.help();
  }
}

