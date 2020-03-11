--
-- PostgreSQL database dump
--

-- Dumped from database version 11.5 (Ubuntu 11.5-0ubuntu0.19.04.1)
-- Dumped by pg_dump version 11.5 (Ubuntu 11.5-0ubuntu0.19.04.1)


--
-- Name: extension; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension (
    id bigint NOT NULL,
    average_rating double precision,
    download_count integer NOT NULL,
    name character varying(255),
    latest_id bigint,
    namespace_id bigint
);


--
-- Name: extension_binary; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_binary (
    id bigint NOT NULL,
    content bytea,
    extension_id bigint
);


--
-- Name: extension_icon; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_icon (
    id bigint NOT NULL,
    content bytea,
    extension_id bigint
);


--
-- Name: extension_license; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_license (
    id bigint NOT NULL,
    content bytea,
    extension_id bigint
);


--
-- Name: extension_readme; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_readme (
    id bigint NOT NULL,
    content bytea,
    extension_id bigint
);


--
-- Name: extension_review; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_review (
    id bigint NOT NULL,
    active boolean NOT NULL,
    comment character varying(2048),
    rating integer NOT NULL,
    "timestamp" timestamp without time zone,
    title character varying(255),
    extension_id bigint,
    user_id bigint
);


--
-- Name: extension_version; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_version (
    id bigint NOT NULL,
    bugs character varying(255),
    description character varying(2048),
    display_name character varying(255),
    extension_file_name character varying(255),
    gallery_color character varying(16),
    gallery_theme character varying(16),
    homepage character varying(255),
    icon_file_name character varying(255),
    license character varying(255),
    license_file_name character varying(255),
    markdown character varying(16),
    preview boolean NOT NULL,
    qna character varying(255),
    readme_file_name character varying(255),
    repository character varying(255),
    "timestamp" timestamp without time zone,
    version character varying(255),
    extension_id bigint,
    published_with_id bigint
);


--
-- Name: extension_version_bundled_extensions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_version_bundled_extensions (
    extension_version_id bigint NOT NULL,
    bundled_extensions_id bigint NOT NULL
);


--
-- Name: extension_version_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_version_categories (
    extension_version_id bigint NOT NULL,
    categories character varying(255)
);


--
-- Name: extension_version_dependencies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_version_dependencies (
    extension_version_id bigint NOT NULL,
    dependencies_id bigint NOT NULL
);


--
-- Name: extension_version_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.extension_version_tags (
    extension_version_id bigint NOT NULL,
    tags character varying(255)
);


--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: namespace; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.namespace (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- Name: namespace_membership; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.namespace_membership (
    id bigint NOT NULL,
    role character varying(32),
    namespace bigint,
    user_data bigint
);


--
-- Name: personal_access_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.personal_access_token (
    id bigint NOT NULL,
    accessed_timestamp timestamp without time zone,
    active boolean NOT NULL,
    created_timestamp timestamp without time zone,
    description character varying(2048),
    value character varying(64),
    user_data bigint
);


--
-- Name: spring_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.spring_session (
    primary_id character(36) NOT NULL,
    session_id character(36) NOT NULL,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval integer NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name character varying(100)
);


--
-- Name: spring_session_attributes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.spring_session_attributes (
    session_primary_id character(36) NOT NULL,
    attribute_name character varying(200) NOT NULL,
    attribute_bytes bytea NOT NULL
);


--
-- Name: user_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_data (
    id bigint NOT NULL,
    avatar_url character varying(255),
    email character varying(255),
    full_name character varying(255),
    login_name character varying(255),
    provider character varying(32),
    provider_id character varying(255),
    provider_url character varying(255)
);


--
-- Name: extension_binary extension_binary_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_binary
    ADD CONSTRAINT extension_binary_pkey PRIMARY KEY (id);


--
-- Name: extension_icon extension_icon_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_icon
    ADD CONSTRAINT extension_icon_pkey PRIMARY KEY (id);


--
-- Name: extension_license extension_license_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_license
    ADD CONSTRAINT extension_license_pkey PRIMARY KEY (id);


--
-- Name: extension extension_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension
    ADD CONSTRAINT extension_pkey PRIMARY KEY (id);


--
-- Name: extension_readme extension_readme_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_readme
    ADD CONSTRAINT extension_readme_pkey PRIMARY KEY (id);


--
-- Name: extension_review extension_review_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_review
    ADD CONSTRAINT extension_review_pkey PRIMARY KEY (id);


--
-- Name: extension_version extension_version_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_version
    ADD CONSTRAINT extension_version_pkey PRIMARY KEY (id);


--
-- Name: namespace_membership namespace_membership_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.namespace_membership
    ADD CONSTRAINT namespace_membership_pkey PRIMARY KEY (id);


--
-- Name: namespace namespace_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.namespace
    ADD CONSTRAINT namespace_pkey PRIMARY KEY (id);


--
-- Name: personal_access_token personal_access_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personal_access_token
    ADD CONSTRAINT personal_access_token_pkey PRIMARY KEY (id);


--
-- Name: spring_session_attributes spring_session_attributes_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spring_session_attributes
    ADD CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name);


--
-- Name: spring_session spring_session_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spring_session
    ADD CONSTRAINT spring_session_pk PRIMARY KEY (primary_id);


--
-- Name: namespace ukeq2y9mghytirkcofquanv5frf; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.namespace
    ADD CONSTRAINT ukeq2y9mghytirkcofquanv5frf UNIQUE (name);


--
-- Name: personal_access_token ukjeud5mssqbqkid58rd2k1inof; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personal_access_token
    ADD CONSTRAINT ukjeud5mssqbqkid58rd2k1inof UNIQUE (value);


--
-- Name: user_data user_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_data
    ADD CONSTRAINT user_data_pkey PRIMARY KEY (id);


--
-- Name: spring_session_ix1; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX spring_session_ix1 ON public.spring_session USING btree (session_id);


--
-- Name: spring_session_ix2; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX spring_session_ix2 ON public.spring_session USING btree (expiry_time);


--
-- Name: spring_session_ix3; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX spring_session_ix3 ON public.spring_session USING btree (principal_name);


--
-- Name: extension_license fk2r233q3pr0ye01mb45dc5dcqh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_license
    ADD CONSTRAINT fk2r233q3pr0ye01mb45dc5dcqh FOREIGN KEY (extension_id) REFERENCES public.extension_version(id);


--
-- Name: extension_version_bundled_extensions fk5c81oqeatr9715wfkrx0w615t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_version_bundled_extensions
    ADD CONSTRAINT fk5c81oqeatr9715wfkrx0w615t FOREIGN KEY (bundled_extensions_id) REFERENCES public.extension(id);


--
-- Name: extension fk64imd3nrj67d50tpkjs94ngmn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension
    ADD CONSTRAINT fk64imd3nrj67d50tpkjs94ngmn FOREIGN KEY (namespace_id) REFERENCES public.namespace(id);


--
-- Name: extension_version_dependencies fk64s9lyasel78kwkpodtedgcv2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_version_dependencies
    ADD CONSTRAINT fk64s9lyasel78kwkpodtedgcv2 FOREIGN KEY (extension_version_id) REFERENCES public.extension_version(id);


--
-- Name: extension_version fk70khj8pm0vacasuiiaq0w0r80; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_version
    ADD CONSTRAINT fk70khj8pm0vacasuiiaq0w0r80 FOREIGN KEY (published_with_id) REFERENCES public.personal_access_token(id);


--
-- Name: extension_version_tags fk8qxmudnllmiyukng19hfkp042; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_version_tags
    ADD CONSTRAINT fk8qxmudnllmiyukng19hfkp042 FOREIGN KEY (extension_version_id) REFERENCES public.extension_version(id);


--
-- Name: extension_version_dependencies fkamd8bx0gf5ju3a7sbx8fnen5v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_version_dependencies
    ADD CONSTRAINT fkamd8bx0gf5ju3a7sbx8fnen5v FOREIGN KEY (dependencies_id) REFERENCES public.extension(id);


--
-- Name: extension fkeqj0wvhffqqvnh4voknohjhtw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension
    ADD CONSTRAINT fkeqj0wvhffqqvnh4voknohjhtw FOREIGN KEY (latest_id) REFERENCES public.extension_version(id);


--
-- Name: extension_binary fkfhwy3ix1g95yh2vktlnt1l96x; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_binary
    ADD CONSTRAINT fkfhwy3ix1g95yh2vktlnt1l96x FOREIGN KEY (extension_id) REFERENCES public.extension_version(id);


--
-- Name: extension_version_categories fkgcqpms03rsk0q4hxx5pbycroq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_version_categories
    ADD CONSTRAINT fkgcqpms03rsk0q4hxx5pbycroq FOREIGN KEY (extension_version_id) REFERENCES public.extension_version(id);


--
-- Name: extension_review fkgd2dqdc23ogbnobx8afjfpnkp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_review
    ADD CONSTRAINT fkgd2dqdc23ogbnobx8afjfpnkp FOREIGN KEY (extension_id) REFERENCES public.extension(id);


--
-- Name: namespace_membership fkgfhwhknula6do2n6wyvqetm3n; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.namespace_membership
    ADD CONSTRAINT fkgfhwhknula6do2n6wyvqetm3n FOREIGN KEY (namespace) REFERENCES public.namespace(id);


--
-- Name: extension_review fkinjbn9grk135y6ik0ut4ujp0w; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_review
    ADD CONSTRAINT fkinjbn9grk135y6ik0ut4ujp0w FOREIGN KEY (user_id) REFERENCES public.user_data(id);


--
-- Name: extension_version fkkhs1ec9s9j08fgicq9pmwu6bt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_version
    ADD CONSTRAINT fkkhs1ec9s9j08fgicq9pmwu6bt FOREIGN KEY (extension_id) REFERENCES public.extension(id);


--
-- Name: extension_readme fknipfwgf0mwn87jbc5bn2giqov; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_readme
    ADD CONSTRAINT fknipfwgf0mwn87jbc5bn2giqov FOREIGN KEY (extension_id) REFERENCES public.extension_version(id);


--
-- Name: namespace_membership fknsamekutxywvsb3s1mjdcjkyp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.namespace_membership
    ADD CONSTRAINT fknsamekutxywvsb3s1mjdcjkyp FOREIGN KEY (user_data) REFERENCES public.user_data(id);


--
-- Name: extension_version_bundled_extensions fkp7o7ws8hrcv24897g2y89f8x5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_version_bundled_extensions
    ADD CONSTRAINT fkp7o7ws8hrcv24897g2y89f8x5 FOREIGN KEY (extension_version_id) REFERENCES public.extension_version(id);


--
-- Name: extension_icon fks849r0hw53a2x8nnwrciho2pp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.extension_icon
    ADD CONSTRAINT fks849r0hw53a2x8nnwrciho2pp FOREIGN KEY (extension_id) REFERENCES public.extension_version(id);


--
-- Name: personal_access_token fktqjvmhoig3wttj6dl1ibcaj3l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personal_access_token
    ADD CONSTRAINT fktqjvmhoig3wttj6dl1ibcaj3l FOREIGN KEY (user_data) REFERENCES public.user_data(id);


--
-- Name: spring_session_attributes spring_session_attributes_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.spring_session_attributes
    ADD CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id) REFERENCES public.spring_session(primary_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

