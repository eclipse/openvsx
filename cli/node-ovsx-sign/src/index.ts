import * as commander from "commander";
import * as fs from "fs";
import smime = require("openssl-smime");

module.exports = function (argv: string[]): void {
  const program = new commander.Command();
  program.usage("<command> [options]");

  const verifyCmd = program.command("verify");
  verifyCmd
    .description("Verify an extension package")
    .option("-p, --extensionpackage <file>", "The extension file path.")
    .option("-a, --signaturearchive <file>", "The signature archive file path.")
    .action(async (options) => {
      const { extensionpackage, signaturearchive } = options;
      const extensionFile = await fs.promises.readFile(extensionpackage);
      // openssl smime -verify -in signedfile.sigzip -CAfile public.pem
      smime.smime(
        "verify",
        {
          in: signaturearchive,
          CAfile: "keys/public.pem",
        },
        extensionFile
      );
    });

    const signCmd = program.command("sign");
    signCmd
      .description("Sign an extension package")
      .option("-p, --extensionpackage <file>", "The extension file path.")
      .option("-k, --privatekey <file>", "The private key to sign the file with.")
      .action(async (options) => {
        const { extensionpackage, privatekey } = options;
        const extensionFile = await fs.promises.readFile(extensionpackage);
        const signature = await smime.smime(
          "sign",
          {
            in: extensionpackage,
            signer: "keys/public.pem",
            inkey: privatekey,
          },
          extensionFile
        );
         await fs.promises.writeFile("./extension.sigzip", signature);
    });

  program.parse(argv);

  if (process.argv.length <= 2) {
    program.help();
  }
};

