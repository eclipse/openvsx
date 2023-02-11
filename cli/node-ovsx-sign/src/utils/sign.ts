import * as crypto from 'crypto';

export const signFile = async (file: Buffer, privateKey: string): Promise<Buffer> => {
    const sign = crypto.createSign('sha256');
    sign.update(file);
    return sign.sign(privateKey);
};
