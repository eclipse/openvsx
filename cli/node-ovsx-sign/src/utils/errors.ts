export class ExtensionSignatureVerificationError extends Error {
    code: number;
    didExecute: boolean;
    constructor(code: number, didExecute: boolean) {
        super();
        this.code = code;
        this.didExecute = didExecute;
    }
}
