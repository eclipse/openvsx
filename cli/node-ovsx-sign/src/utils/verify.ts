import * as crypto from 'crypto';
import { SIGNING_ALGORITHM } from './constants';

export const verifySignature = async (file: Buffer, publicKey: string, signature: Buffer): Promise<boolean> => {
    const verify = crypto.createVerify(SIGNING_ALGORITHM);
    verify.update(file);
    return verify.verify(publicKey, signature);
};
