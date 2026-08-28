---
name: tanstack-query-conventions
description: How TanStack Query is used in the webui — hook shape, query keys, retries, mutations, options objects, and how consumers read results. Use when writing or reviewing any useQuery/useMutation/useInfiniteQuery code.
---

# TanStack Query conventions

## Hooks, not raw queries

- Never call `useQuery` / `useMutation` / `useInfiniteQuery` directly in a component — wrap each endpoint in a `use*` hook.
- The hook returns the **react-query result object unchanged**. Don't return just `data` or a hand-picked subset; consumers destructure what they need.
- Co-locate a new hook in the feature's folder; promote to `src/hooks/` only when a second place needs it (see AGENTS.md).

## Query hook shape

```ts
export const useUserExtension = (target: UserExtensionTarget) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: ['user', 'extension', target.namespace, target.extension],
        queryFn: ({ signal }) =>
            service.getExtension(controllerFromSignal(signal), target.namespace, target.extension)
    });
};
```

- The `queryFn` is a one-liner because the service method rejects on failure — see "Errors belong to the service" below. Never re-check `isError` in a hook.
- `controllerFromSignal(signal)` (`query-client.ts`) bridges TanStack's `AbortSignal` to the `AbortController` the service expects — service signatures stay untouched, component-level `AbortController` refs go away.
- `useQuery` forbids `undefined`; normalise a "no result" case to `null`.
- Query keys are hierarchical arrays (`['admin', 'namespace', name]`). When a key is reused for invalidation, export a small `*Keys` helper next to the hook.

## Mutation hook shape

```ts
export const useCreateNamespace = () => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (name: string) => service.admin.createNamespace({ name }),
        onSuccess: (_result, name) => queryClient.invalidateQueries({ queryKey: namespaceAdminKeys.detail(name) })
    });
};
```

- **No `AbortController` / signal in mutations** — we don't abort writes anymore.
- Mutations don't retry (TanStack's default `retry: 0`), which is correct for non-idempotent writes.
- The `mutationFn` forwards the service call directly; the service rejects on failure, so `mutateAsync` callers get their `catch` / `onError` path for free.
- Invalidate or remove affected queries in `onSuccess`.

## Errors belong to the service, not the hook

The registry answers some failures with a `200` carrying an `{ error: '…' }` body instead of a non-2xx status. `sendStrictRequest` (`server-request.ts`) is the single place that normalises this: it is non-retriable *and* rejects on such a body, so a migrated service method resolves with data or rejects — never both.

```ts
// extension-registry-service.ts — migrated methods
async getNamespace(abortController: AbortController, name: string): Promise<Readonly<Namespace>> {
    return sendStrictRequest({ abortController, credentials: true, endpoint: /* … */ });
}
```

- A migrated method's return type **drops `| ErrorResult`** — that union is what forced the `isError` check on every caller.
- Hooks therefore never contain `if (isError(result)) throw result`. If you find yourself writing one, the service method still needs migrating.
- `sendRequest` / `sendNonRetriableRequest` keep resolving error bodies; legacy, un-migrated consumers check `isError` themselves. Don't change their behaviour.
- Same rule in tests: stub a failing service method with `mockRejectedValue({ error: '…' })`, not `mockResolvedValue`.

## Retries and caching are owned by the shared client

- One singleton `queryClient` (`query-client.ts`) retries network/5xx with backoff, never 4xx; 429s are waited out inside `sendRequest`. Migrated service methods use `sendStrictRequest` (retries disabled), so this is the only retry layer.
- Defaults: `refetchOnWindowFocus: false`, `staleTime: 60s`. Override per hook only with reason — `staleTime: 0` / `gcTime: 0` when data must always be fresh (right after publish/delete), `retry: false` to let a 404 surface immediately.

## Options objects, not positional flags

When a hook needs optional behaviour, take an options object that forwards TanStack's options — never a bare positional flag:

```ts
export const useThing = (id: string, options?: Omit<UseQueryOptions<Thing>, 'queryKey' | 'queryFn'>) =>
    useQuery({ queryKey: ['thing', id], queryFn: /* … */, ...options });
```

## Naming

- `use<Thing>`, `useCreate*`, `useDelete*`, `useChange*`; infinite scroll → `useInfinite*` backed by `useInfiniteQuery`.
- Don't prefix admin hooks with "admin" — the `service.admin.*` namespace and the `admin-dashboard/` folder already convey it.

## Consumer side — always destructure and rename

```ts
const { data: user, error: userError, isFetching } = useUserExtension(target);
const { mutateAsync: createNamespace, isPending: creating } = useCreateNamespace();
```

Never read `result.data` or call `result.mutate` off an undestructured object. Destructure and give the fields meaningful names — especially for mutations, where a bare `mutate` / `isPending` says nothing at the call site.

The one exception: if you need to **forward** the query result to another function or component, pass the whole result object — don't destructure and re-pass individual fields, which hands over a point-in-time snapshot instead of the live result.

## Best practices

- **`queryOptions` for shared queries** — when the same query is read from more than one place, define it once with TanStack's `queryOptions({ queryKey, queryFn })` and spread it into the hook, for type-safe reuse. (Not used yet — adopt when a query gains a second reader.)
- **`staleTime`** — the shared client sets `60s`; raise it for rarely-changing data, drop to `0` for reads that must always refetch on mount. Keep `staleTime <= gcTime` (see pitfalls).
- **Pagination** keeps the previous page with `placeholderData: keepPreviousData` (as `use-infinite-search` does), never `initialData`.
- **`enabled` for dependent queries** — gate a query through the options object (`{ enabled: !!id }`), never by calling the hook conditionally.
- **`select` for derived data** — transform in `select` inside the query config, not in the component. The hook still returns the whole result (only `data`'s value changes), so this doesn't break "return the result unchanged".
- **Pure `queryFn`** — fetch and normalise only, no side effects. Deliberate exception: the extension-icon query produces an object URL that `query-client.ts` revokes on cache eviction.
- **Suspense** — only when a component sits under a Suspense boundary, `useSuspenseQuery` drops the loading/`undefined` branches. Our hooks default to plain `useQuery` with explicit loading/error handling; don't switch unless the boundary exists.
- **Mutations invalidate, they don't rely on optimism** — always invalidate affected queries after a mutation (see the mutation shape above); optimistic updates are an addition, not a replacement. If you do optimistic updates, `cancelQueries` in `onMutate` before writing and snapshot for rollback in `onError`.
- **Tests** — render through the `test-providers` harness: a fresh client per render with `retry: false` and `gcTime: Infinity` (so a query isn't collected mid-assertion). That covers "retry: false in tests", "gcTime: Infinity in tests", and "never share one client across tests"; don't hand-roll a client.

## Common pitfalls

- **`initialData` when you mean `placeholderData`** — `initialData` is treated as fresh (subject to `staleTime`) and written to cache; `placeholderData` is display-only.
- **Infinite query without `initialPageParam`** — required in v5.
- **Conditional hook calls** — gate with `enabled`, never wrap the hook in an `if`.
- **Optimistic update without cancelling first** — an in-flight refetch lands after your write and clobbers it; `cancelQueries` in `onMutate`.
- **`staleTime` > `gcTime`** — data is garbage-collected while still "fresh", causing surprise refetches.
- **Fire-and-forget invalidation when order matters** — the mutation-shape example doesn't await its `invalidateQueries`; **return** the promise from `onSuccess` when the mutation should stay pending until the refetch completes.
