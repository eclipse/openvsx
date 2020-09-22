ALTER TABLE public.file_resource
    ADD name character varying(255),
    ADD storage_type character varying(32);

UPDATE public.file_resource
    SET name = extension_version.extension_file_name
    FROM public.extension_version
    WHERE file_resource.extension_id = extension_version.id AND file_resource.type = 'download';

UPDATE public.file_resource
    SET name = extension_version.readme_file_name
    FROM public.extension_version
    WHERE file_resource.extension_id = extension_version.id AND file_resource.type = 'readme';

UPDATE public.file_resource
    SET name = extension_version.license_file_name
    FROM public.extension_version
    WHERE file_resource.extension_id = extension_version.id AND file_resource.type = 'license';

UPDATE public.file_resource
    SET name = extension_version.icon_file_name
    FROM public.extension_version
    WHERE file_resource.extension_id = extension_version.id AND file_resource.type = 'icon';

UPDATE public.file_resource
    SET name = 'package.json'
    WHERE file_resource.type = 'manifest';

UPDATE public.file_resource
    SET storage_type = 'database';

ALTER TABLE public.extension_version 
    DROP COLUMN extension_file_name,
    DROP COLUMN readme_file_name,
    DROP COLUMN license_file_name,
    DROP COLUMN icon_file_name;
