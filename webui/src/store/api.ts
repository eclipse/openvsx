import { BaseQueryFn, createApi, FetchArgs, fetchBaseQuery, FetchBaseQueryError } from '@reduxjs/toolkit/query/react';
import { CsrfTokenJson, ErrorResult, Extension, ExtensionFilter, ExtensionReviewList, isError, isSuccess, LoginProviders, MembershipRole, Namespace, NamespaceDetails, NamespaceMembershipList, NewReview, PersonalAccessToken, PublisherInfo, RegistryVersion, SearchEntry, SearchResult, SuccessResult, UrlString, UserData } from '../extension-registry-types';
import { createAbsoluteURL } from '../utils';

let serverHost = location.host;
if (serverHost.startsWith('3000-')) {
    // Gitpod dev environment: the frontend runs on port 3000, but the server runs on port 8080
    serverHost = '8080-' + serverHost.substring(5);
} else if (location.port === '3000') {
    // Localhost dev environment
    serverHost = location.hostname + ':8080';
} else if (serverHost.includes('che-webui')) {
    // Eclipse Che dev environment.
    // If serverHost contains 'che-webui', replace it with 'che-server'
    serverHost = serverHost.replace('che-webui', 'che-server');
}

export const serverUrl = `${location.protocol}//${serverHost}`;
export const logoutUrl = createAbsoluteURL([serverUrl, 'logout']);
export const eclipseLoginUrl = createAbsoluteURL([serverUrl, 'oauth2', 'authorization', 'eclipse']);

const baseQueryWithCsrf: BaseQueryFn<
    string | FetchArgs,
    unknown,
    FetchBaseQueryError
> = async (args, api, extraOptions) => {
    let token: CsrfTokenJson | undefined = undefined;
    // TODO double-check when to use CSRF, not only for mutations
    if (api.type === 'mutation') {
        const { data } = await api.dispatch(apiSlice.endpoints.getCsrfToken.initiate(undefined, { forceRefetch: true }));
        if (data && !isError(data)) {
            token = data as CsrfTokenJson;
        }
    }
    return await fetchBaseQuery({
        baseUrl: serverUrl,
        prepareHeaders: (headers) => {
            if (token != null) {
                headers.set(token.header, token.value);
            }

            return headers;
        },
    })(args, api, extraOptions);
};

export const apiSlice = createApi({
    reducerPath: 'api',
    baseQuery: baseQueryWithCsrf,
    tagTypes: [
        'RegistryVersion',
        'LoginProviders',
        'User',
        'UserByName',
        'UserAuthError',
        'NamespaceDetails',
        'Extension',
        'ExtensionReadme',
        'ExtensionChangelog',
        'ExtensionIcon',
        'ExtensionReviews',
        'AccessTokens',
        'UserNamespaces',
        'NamespaceMembers',
        'StaticContent',
        'AdminExtension',
        'AdminNamespace',
        'AdminPublisherInfo'
    ],
    endpoints: builder => ({
        getRegistryVersion: builder.query<Readonly<RegistryVersion>, void>({
            query: () => '/api/version',
            transformErrorResponse: () => {
                console.error('Could not determine server version');
                return 'unknown';
            },
            providesTags: ['RegistryVersion']
        }),
        getLoginProviders: builder.query<Readonly<Record<string, string>>, void>({
            query: () => '/login-providers',
            transformResponse: (response: Readonly<LoginProviders | SuccessResult>) => {
                if (isSuccess(response)) {
                    console.log(response.success);
                    return {};
                } else {
                    return (response as LoginProviders).loginProviders;
                }
            },
            providesTags: ['LoginProviders']
        }),
        getUser: builder.query<Readonly<UserData | undefined>, void>({
            query: () => ({
                url: '/user',
                credentials: 'include'
            }),
            transformResponse: (response: Readonly<UserData | ErrorResult>) => {
                return !isError(response) ? response as UserData : undefined;
            },
            providesTags: ['User']
        }),
        getUserAuthError: builder.query<Readonly<ErrorResult>, void>({
            query: () => ({
                url: '/user/auth-error',
                credentials: 'include'
            }),
            providesTags: ['UserAuthError']
        }),
        getCsrfToken: builder.query<Readonly<CsrfTokenJson | ErrorResult>, void>({
            query: () => ({
                url: '/user/csrf',
                credentials: 'include'
            }),
        }),
        signPublisherAgreement: builder.mutation<Readonly<UserData | ErrorResult>, void>({
            query: () => ({
                url: '/user/publisher-agreement',
                method: 'POST',
                credentials: 'include'
            }),
            invalidatesTags: ['User']
        }),
        getNamespaceDetails: builder.query<Readonly<NamespaceDetails>, string>({
            query: (name) => `/api/${name}/details`,
            providesTags: (details) => details != null ? [{ type: 'NamespaceDetails', id: details.name }] : []
        }),
        setNamespaceDetails: builder.mutation<Readonly<SuccessResult | ErrorResult>, { endpoint: string, details: NamespaceDetails }>({
            query: (input) => ({
                url: input.endpoint,
                body: input.details,
                method: 'POST',
                credentials: 'include'
            }),
            invalidatesTags: (result, error, input) => isSuccess(result) ? [{ type: 'NamespaceDetails', id: input.details.name }] : []
        }),
        setNamespaceLogo: builder.mutation<Readonly<SuccessResult | ErrorResult>, { endpoint: string, name: string, logoFile: Blob, logoName: string }>({
            query: (input) => {
                const form = new FormData();
                form.append('file', input.logoFile, input.logoName);
                return {
                    url: input.endpoint,
                    body: form,
                    method: 'POST',
                    credentials: 'include'
                };
            },
            invalidatesTags: (result, error, input) => isSuccess(result) ? [{ type: 'NamespaceDetails', id: input.name }] : []
        }),
        search: builder.query<Readonly<SearchResult | ErrorResult>, ExtensionFilter | undefined>({
            query: (filter) => {
                let params: Record<string, any> | undefined = undefined;
                if (filter) {
                    params = {};
                    if (filter.query)
                        params['query'] = filter.query;
                    if (filter.category)
                        params['category'] = filter.category;
                    if (filter.offset)
                        params['offset'] = filter.offset;
                    if (filter.size)
                        params['size'] = filter.size;
                    if (filter.sortBy)
                        params['sortBy'] = filter.sortBy;
                    if (filter.sortOrder)
                        params['sortOrder'] = filter.sortOrder;
                }

                return {
                    url: '/api/-/search',
                    params
                };
            }
        }),
        getExtensionDetail: builder.query<Readonly<Extension|undefined>, {namespace: string, name: string, target?: string, version?: string}>({
            query: ({ namespace, name, target, version }) => {
                const params = [namespace, name];
                if (target) {
                    params.push(target);
                }
                if (version) {
                    params.push(version);
                }
                return { url: `/api/${params.join('/')}` };
            },
            transformResponse: (response: Readonly<Extension | ErrorResult>) => {
                return !isError(response) ? response as Extension : undefined;
            },
            providesTags: (result) => {
                if (result != null && !isError(result)) {
                    const extension = result as Extension;
                    const id = `${extension.namespace}.${extension.name}`;
                    return [{ type: 'Extension', id }];
                } else {
                    return [];
                }
            }
        }),
        getExtensionIcon: builder.query<Blob | undefined, Extension | SearchEntry>({
            query: (extension) => ({
                url: extension.files.readme,
                headers: { 'Accept': 'application/octet-stream' },
                redirect: 'follow'
            }),
            providesTags: (result, error, extension) => result != null ? [{ type: 'ExtensionIcon', id: `${extension.namespace}.${extension.name}` }] : []
        }),
        getExtensionReviews: builder.query<Readonly<ExtensionReviewList>, Extension>({
            query: (extension) => extension.reviewsUrl,
            providesTags: (result, error, extension) => result != null ? [{ type: 'ExtensionReviews', id: `${extension.namespace}.${extension.name}` }] : []
        }),
        postReview: builder.mutation<Readonly<SuccessResult | ErrorResult>, { review: NewReview, postReviewUrl: UrlString, extension: Extension }>({
            query: ({ review, postReviewUrl }) => ({
                url: postReviewUrl,
                method: 'POST',
                body: review,
                credentials: 'include'
            }),
            invalidatesTags: (result, error, { extension }) => {
                if (isSuccess(result)) {
                    const id = `${extension.namespace}.${extension.name}`;
                    return [{ type: 'ExtensionReviews', id }, { type: 'Extension', id }];
                } else {
                    return [];
                }
            }
        }),
        deleteReview: builder.mutation<Readonly<SuccessResult | ErrorResult>, { deleteReviewUrl: UrlString, extension: Extension }>({
            query: ({ deleteReviewUrl }) => ({
                url: deleteReviewUrl,
                method: 'POST',
                credentials: 'include'
            }),
            invalidatesTags: (result, error, { extension }) => {
                if (isSuccess(result)) {
                    const id = `${extension.namespace}.${extension.name}`;
                    return [{ type: 'ExtensionReviews', id }, { type: 'Extension', id }];
                } else {
                    return [];
                }
            }
        }),
        getUserByName: builder.query<Readonly<UserData>[], string>({
            query: (name) => ({
                url: `/user/search/${name}`,
                credentials: 'include'
            }),
            providesTags: (result, error, name) => result != null ? [{ type: 'UserByName', id: name }] : []
        }),
        getAccessTokens: builder.query<Readonly<PersonalAccessToken>[], UserData>({
            query: (user) => ({
                url: user.tokensUrl,
                credentials: 'include'
            }),
            providesTags: ['AccessTokens']
        }),
        createAccessToken: builder.mutation<Readonly<PersonalAccessToken>, { user: UserData, description: string }>({
            query: ({ user, description }) => ({
                url: user.createTokenUrl,
                params: { description },
                method: 'POST',
                credentials: 'include'
            }),
            invalidatesTags: (result) => result != null ? ['AccessTokens'] : []
        }),
        deleteAccessToken: builder.mutation<Readonly<SuccessResult | ErrorResult>, PersonalAccessToken>({
            query: (token) => ({
                url: token.deleteTokenUrl,
                method: 'POST',
                credentials: 'include'
            }),
            invalidatesTags: (result) => result != null && isSuccess(result) ? ['AccessTokens'] : []
        }),
        deleteAllAccessTokens: builder.mutation<Readonly<SuccessResult | ErrorResult>[], UserData>({
            query: (user) => ({
                url: user.deleteAllTokensUrl,
                method: 'POST',
                credentials: 'include'
            }),
            invalidatesTags: (result) => result != null ? ['AccessTokens'] : []
        }),
        getNamespaces: builder.query<Readonly<Namespace>[], void>({
            query: () => ({
                url: '/user/namespaces',
                credentials: 'include'
            }),
            providesTags: ['UserNamespaces']
        }),
        getNamespaceMembers: builder.query<Readonly<NamespaceMembershipList>, Namespace>({
            query: (namespace) => ({
                url: namespace.membersUrl,
                credentials: 'include'
            }),
            providesTags: ['NamespaceMembers']
        }),
        setNamespaceMember: builder.mutation<Readonly<SuccessResult | ErrorResult>[], { endpoint: UrlString, user: UserData, role: MembershipRole | 'remove' }>({
            query: ({ endpoint, user, role }) => ({
                url: endpoint,
                params: {
                    user: user.loginName,
                    provider: user.provider,
                    role
                },
                method: 'POST',
                credentials: 'include'
            }),
            invalidatesTags: (results) => results != null && results.some((result) => isSuccess(result)) ? ['NamespaceMembers'] : []
        }),
        getStaticContent: builder.query<string, string>({
            query: (url) => ({
                url,
                headers: { 'Accept': 'text/plain' },
                redirect: 'follow'
            }),
            providesTags: (result, error, url) => result != null ? [{ type: 'StaticContent', id: url }] : []
        }),
        publishExtension: builder.mutation<Readonly<Extension | ErrorResult>, File>({
            query: (extensionPackage) => ({
                url: '/api/user/publish',
                method: 'POST',
                payload: extensionPackage,
                headers: { 'Content-Type': 'application/octet-stream' },
                credentials: 'include'
            }),
            invalidatesTags: [{ type: 'Extension', id: 'List' }]
        }),
        createNamespace: builder.mutation<Readonly<SuccessResult | ErrorResult>, string>({
            query: (name) => ({
                url: '/api/user/namespace/create',
                method: 'POST',
                payload: { name },
                credentials: 'include'
            }),
            invalidatesTags: ['UserNamespaces']
        }),
        getExtensions: builder.query<Readonly<Extension[] | undefined>, void>({
            query: () => ({
                url: '/user/extensions',
                credentials: 'include'
            }),
            transformResponse: (response: Readonly<Extension[] | ErrorResult>) => {
                return !isError(response) ? response as Extension[] : undefined;
            },
            providesTags: [{ type: 'Extension', id: 'List' }]
        }),
        getExtension: builder.query<Readonly<Extension>, { namespace: string, extension: string }>({
            query: ({ namespace, extension }) => ({
                url: `/user/extension/${namespace}/${extension}`,
                credentials: 'include'
            }),
            providesTags: (result) => result != null ? [{ type: 'Extension', id: `${result.namespace}.${result.name}` }] : []
        }),
        deleteExtensions: builder.mutation<Readonly<SuccessResult | ErrorResult>, { namespace: string, extension: string, targetPlatformVersions?: object[] }>({
            query: ({ namespace, extension, targetPlatformVersions }) => ({
                url: `/user/extension/${namespace}/${extension}/delete`,
                method: 'POST',
                credentials: 'include',
                payload: targetPlatformVersions
            }),
            invalidatesTags: (result, error, { namespace, extension }) => {
                if (result != null && isSuccess(result)) {
                    const id = `${namespace}.${extension}`;
                    return [{ type: 'Extension', id }, { type: 'AdminExtension', id }];
                } else {
                    return [];
                }
            }
        }),
        adminGetExtension: builder.query<Readonly<Extension>, { namespace: string, extension: string }>({
            query: ({ namespace, extension }) => ({
                url: `/admin/extension/${namespace}/${extension}`,
                credentials: 'include'
            }),
            providesTags: (result) => result != null ? [{ type: 'AdminExtension', id: `${result.namespace}.${result.name}` }] : []
        }),
        adminDeleteExtensions: builder.mutation<Readonly<SuccessResult | ErrorResult>, { namespace: string, extension: string, targetPlatformVersions?: object[] }>({
            query: ({ namespace, extension, targetPlatformVersions }) => ({
                url: `/admin/extension/${namespace}/${extension}/delete`,
                method: 'POST',
                credentials: 'include',
                payload: targetPlatformVersions
            }),
            invalidatesTags: (result, error, { namespace, extension }) => {
                if (result != null && isSuccess(result)) {
                    const id = `${namespace}.${extension}`;
                    return [{ type: 'Extension', id }, { type: 'AdminExtension', id }];
                } else {
                    return [];
                }
            }
        }),
        adminGetNamespace: builder.query<Readonly<Namespace>, string>({
            query: (name) => ({
                url: `/admin/namespace/${name}`,
                credentials: 'include'
            }),
            providesTags: (result) => result != null ? [{ type: 'AdminNamespace', id: result.name }] : []
        }),
        adminCreateNamespace: builder.mutation<Readonly<SuccessResult | ErrorResult>, { name: string }>({
            query: (namespace) => ({
                url: '/admin/create-namespace',
                method: 'POST',
                payload: namespace,
                credentials: 'include'
            })
        }),
        adminChangeNamespace: builder.mutation<Readonly<SuccessResult | ErrorResult>, { oldNamespace: string, newNamespace: string, removeOldNamespace: boolean, mergeIfNewNamespaceAlreadyExists: boolean }>({
            query: (req) => ({
                url: '/admin/change-namespace',
                method: 'POST',
                payload: req,
                credentials: 'include'
            }),
            invalidatesTags: (result, error, req) => result != null && isSuccess(result) ? [{ type: 'AdminNamespace', id: req.oldNamespace }, { type: 'AdminNamespace', id: req.newNamespace }, { type: 'NamespaceDetails', id: req.oldNamespace }, { type: 'NamespaceDetails', id: req.newNamespace }] : []
        }),
        adminGetPublisherInfo: builder.query<Readonly<PublisherInfo>, { provider: string, login: string }>({
            query: ({ provider, login }) => ({
                url: `'/admin/publisher/${provider}/${login}`,
                credentials: 'include'
            }),
            providesTags: (result) => result != null ? [{ type: 'AdminPublisherInfo', id: `${result.user.provider}/${result.user.loginName}` }] : []
        }),
        adminRevokePublisherContributions: builder.mutation<Readonly<SuccessResult | ErrorResult>, { provider: string, login: string }>({
            query: ({ provider, login }) => ({
                url: `/admin/publisher/${provider}/${login}/revoke`,
                method: 'POST',
                credentials: 'include'
            }),
            invalidatesTags: (result, error, { provider, login }) => result != null && isSuccess(result) ? [{ type: 'AdminPublisherInfo', id: `${provider}/${login}` }] : []
        }),
        adminRevokeAccessTokens: builder.mutation<Readonly<SuccessResult | ErrorResult>, { provider: string, login: string }>({
            query: ({ provider, login }) => ({
                url: `/admin/publisher/${provider}/${login}/tokens/revoke`,
                method: 'POST',
                credentials: 'include'
            }),
            invalidatesTags: (result, error, { provider, login }) => result != null && isSuccess(result) ? [{ type: 'AdminPublisherInfo', id: `${provider}/${login}` }] : []
        })
    })
});

export const {
    useGetCsrfTokenQuery,
    useGetRegistryVersionQuery,
    useGetLoginProvidersQuery,
    useGetUserQuery,
    useGetUserAuthErrorQuery,
    useSignPublisherAgreementMutation,
    useGetNamespaceDetailsQuery,
    useSetNamespaceDetailsMutation,
    useSetNamespaceLogoMutation,
    useSearchQuery,
    useGetExtensionDetailQuery,
    useGetExtensionIconQuery,
    useGetExtensionReviewsQuery,
    usePostReviewMutation,
    useDeleteReviewMutation,
    useGetUserByNameQuery,
    useGetAccessTokensQuery,
    useCreateAccessTokenMutation,
    useDeleteAccessTokenMutation,
    useDeleteAllAccessTokensMutation,
    useGetNamespacesQuery,
    useGetNamespaceMembersQuery,
    useSetNamespaceMemberMutation,
    useGetStaticContentQuery,
    usePublishExtensionMutation,
    useCreateNamespaceMutation,
    useGetExtensionsQuery,
    useGetExtensionQuery,
    useDeleteExtensionsMutation,
    useAdminGetExtensionQuery,
    useAdminDeleteExtensionsMutation,
    useAdminGetNamespaceQuery,
    useAdminCreateNamespaceMutation,
    useAdminChangeNamespaceMutation,
    useAdminGetPublisherInfoQuery,
    useAdminRevokePublisherContributionsMutation,
    useAdminRevokeAccessTokensMutation
} = apiSlice;