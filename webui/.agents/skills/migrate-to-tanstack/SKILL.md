---
name: migrate-to-tanstack
description: Migrate a data-fetching endpoint from the legacy ExtensionRegistryService + fetch-retry pattern to a TanStack Query hook. Only do this when the user explicitly asks — it is an opt-in, in-progress migration. Pairs with the tanstack-query-conventions skill.
---

# Migrating an endpoint to TanStack Query

**Only run this when the user explicitly asks to migrate an endpoint.** The migration is in progress and opt-in — never convert endpoints as a side effect of other work.

## The current picture

`src/extension-registry-service.ts` (`ExtensionRegistryService`, plus `service.admin.*`) holds every server call. Legacy, un-migrated consumers call these methods straight from a component, passing an `AbortController` and relying on `fetch-retry`'s 10-attempt backoff inside `sendRequest`. That drags along per-component `AbortController` refs, `useEffect` fetch-on-mount wiring, and hand-rolled loading/error state.

Migrated endpoints instead go through a `use*` hook wrapping `useQuery`/`useMutation`, and retries move to the shared query client (`src/query-client.ts`). Roughly half the service is migrated — grep before assuming either state.

## Steps

1. **Find every consumer** of the method you're migrating: `grep -rn "service\.<method>\|\.<method>(" src`. List them — you'll migrate all of them or a named subset.

2. **Decide the retry scope — ask if unsure.** Ideally the service method flips from `sendRequest` (retriable) to `sendNonRetriableRequest`, handing retries to TanStack. Only do that when **every** consumer is moving to a hook — a legacy consumer still calling the method directly would silently lose its retry. If you're migrating just one of several consumers, either leave the method retriable (the query then double-retries, tolerated in the interim) or confirm scope with the user. When the request doesn't make the consumer scope clear, ask.

3. **Adjust the service method.**
   - Query methods: keep the `AbortController` param — the hook passes `controllerFromSignal(signal)`.
   - Mutation methods: **drop the `AbortController` param** — we no longer abort writes.

4. **Create the hook** — shape and naming per the `tanstack-query-conventions` skill. Co-locate it in the feature's folder first; move to `src/hooks/` only when a second place needs it. The hook returns the react-query result object **as-is**, never just `data` or a picked subset.

5. **Update the consumers.** Replace the `AbortController` / `useEffect` / manual-state boilerplate with the hook, destructuring and renaming its result (`const { data: user, error: userError } = ...`; `const { mutateAsync, isPending } = ...`). Delete the dead boilerplate.

6. **Finish per the `write-code` skill:** add or update tests (`write-tests`), add a changelog entry, and pass `yarn lint`.

## Don't

- Don't strip `fetch-retry` from `sendRequest` globally — that's the final cleanup once the whole migration is done, and un-migrated direct callers still depend on it. Keep the 429 / `Retry-After` block regardless.
- Don't migrate endpoints nobody asked you to.
