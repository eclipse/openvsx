import * as fs from 'fs';
import * as https from "https";
import * as os from "os";
import * as path from "path";

export const loadPrivateKey = (keyPath: string): Promise<string> => {
  return fs.promises.readFile(keyPath, 'utf8');
};

export const loadPublicKey = (keyPath: string): Promise<string> => {
  return fs.promises.readFile(keyPath, 'utf8');
};

export const downloadPublicKey = async (): Promise<string> => {
    // Todo: Replace this with the real URL
    const urlOfPublicKey = "https://files.interclip.app/public_key_ovsx_sign.pem";
    console.log("Downloading public key from", urlOfPublicKey);
    const publicKey = await new Promise((resolve, reject) => {
        https
            .get(urlOfPublicKey, (res) => {
                let data = "";
                res.on("data", (chunk) => {
                    data += chunk;
                });
                res.on("end", () => {
                    resolve(data);
                });
            })
            .on("error", (err) => {
                reject(err);
            });
    });

    const downloadLocation = path.join(
        os.tmpdir(),
        `ovsx-sign-keys/public_key.pem`
    );
    await fs.promises.mkdir(path.dirname(downloadLocation), { recursive: true });
    console.info("Writing public key to", downloadLocation);

    // Write the public key to a file
    await fs.promises.writeFile(downloadLocation, publicKey);
    return downloadLocation;
};

