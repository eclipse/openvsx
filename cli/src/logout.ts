import { openDefaultStore } from "./store";

export default async function logout(namespaceName: string) {
	if (!namespaceName) {
        throw new Error('Missing namespace name.');
    }

	const store = await openDefaultStore();
	if (!store.get(namespaceName)) {
		throw new Error(`Unknown namespace '${namespaceName}'.`);
	}

	await store.delete(namespaceName);
    console.log(`\ud83d\ude80  ${namespaceName} removed from the list of known namespaces`);
}