import * as commander from "commander";
import { verify, sign } from "./utils/sign";

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
    .option("-o, --output <output>", "The path to the output signature archive")
    .action(sign);

  program.parse(argv);

  if (process.argv.length <= 2) {
    program.help();
  }
}

