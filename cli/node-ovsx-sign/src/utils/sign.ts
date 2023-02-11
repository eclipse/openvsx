import * as crypto from 'crypto';
import { SIGNING_ALGORITHM } from './constants';

export const signFile = async (file: Buffer, privateKey: string): Promise<Buffer> => {
    const sign = crypto.createSign(SIGNING_ALGORITHM);
    sign.update(file);
    return sign.sign(privateKey);
};
