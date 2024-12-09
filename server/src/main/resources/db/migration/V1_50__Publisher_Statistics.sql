-- Create tables and constraints for PublisherStatistics entity
CREATE TABLE public.publisher_statistics(
    id bigint NOT NULL,
    user_data bigint NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    downloads BIGINT NOT NULL,
    downloads_total BIGINT NOT NULL
);

ALTER TABLE ONLY public.publisher_statistics ADD CONSTRAINT publisher_statistics_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.publisher_statistics ADD CONSTRAINT fk_publisher_statistics_user_data FOREIGN KEY (user_data) REFERENCES public.user_data(id);
CREATE SEQUENCE publisher_statistics_seq INCREMENT 50 OWNED BY public.publisher_statistics.id;