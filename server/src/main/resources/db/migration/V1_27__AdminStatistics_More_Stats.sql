CREATE TABLE public.admin_statistics_top_most_active_publishing_users(
    admin_statistics_id BIGINT NOT NULL,
    login_name CHARACTER VARYING(255) NOT NULL,
    extension_version_count INT NOT NULL
);

ALTER TABLE ONLY public.admin_statistics_top_most_active_publishing_users
    ADD CONSTRAINT admin_statistics_top_most_active_publishing_users_fkey FOREIGN KEY (admin_statistics_id) REFERENCES admin_statistics(id);

CREATE TABLE public.admin_statistics_top_namespace_extensions(
    admin_statistics_id BIGINT NOT NULL,
    namespace CHARACTER VARYING(255) NOT NULL,
    extension_count INT NOT NULL
);

ALTER TABLE ONLY public.admin_statistics_top_namespace_extensions
    ADD CONSTRAINT admin_statistics_top_namespace_extensions_fkey FOREIGN KEY (admin_statistics_id) REFERENCES admin_statistics(id);

CREATE TABLE public.admin_statistics_top_namespace_extension_versions(
    admin_statistics_id BIGINT NOT NULL,
    namespace CHARACTER VARYING(255) NOT NULL,
    extension_version_count INT NOT NULL
);

ALTER TABLE ONLY public.admin_statistics_top_namespace_extension_versions
    ADD CONSTRAINT admin_statistics_top_namespace_extension_versions_fkey FOREIGN KEY (admin_statistics_id) REFERENCES admin_statistics(id);

CREATE TABLE public.admin_statistics_top_most_downloaded_extensions(
    admin_statistics_id BIGINT NOT NULL,
    extension_identifier CHARACTER VARYING(255) NOT NULL,
    downloads BIGINT NOT NULL
);

ALTER TABLE ONLY public.admin_statistics_top_most_downloaded_extensions
    ADD CONSTRAINT admin_statistics_top_most_downloaded_extensions_fkey FOREIGN KEY (admin_statistics_id) REFERENCES admin_statistics(id);
