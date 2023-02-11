import * as crypto from 'crypto';

export const verifySignature = async (file: Buffer, publicKey: string, signature: Buffer): Promise<boolean> => {
    const verify = crypto.createVerify('sha256');
    verify.update(file);
    return verify.verify(publicKey, signature);
};
