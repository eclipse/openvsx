-- Append-only log of the publicly visible transitions of extension versions, backing the registry
-- changes feed (/api/-/version-changes). One row per transition, so a version that is published,
-- withdrawn and reinstated is reported three times rather than as a single entry that moves around.
-- Consumers such as mirrors or security scanners can therefore replay the full history, and never miss
-- a transition that happened between two of their polls.
--
-- The coordinates of the version are copied into every row instead of being joined in from
-- extension_version. An entry records something that happened, and has to stay readable after the
-- version it talks about is gone: purging a version is itself reported, and joining against a row that
-- no longer exists would drop that entry and the version's entire history along with it.
CREATE SEQUENCE IF NOT EXISTS extension_version_change_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE public.extension_version_change
(
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('extension_version_change_seq'),

    -- NULL once the version has been purged; the coordinates below keep identifying it.
    extension_version_id BIGINT,

    namespace CHARACTER VARYING(255) NOT NULL,
    extension CHARACTER VARYING(255) NOT NULL,
    version CHARACTER VARYING(255) NOT NULL,
    target_platform CHARACTER VARYING(255),

    state CHARACTER VARYING(32) NOT NULL,

    -- When the version was published, reported by the feed as 'timestamp'. Copied from the version, so
    -- it is the same on all of its entries, and absent for the versions that carry no timestamp.
    timestamp TIMESTAMP,

    -- When the transition happened, reported by the feed as 'lastUpdated'. The feed is ordered by this.
    changed_at TIMESTAMP NOT NULL,

    -- constraints

    -- Purging a version detaches its entries rather than deleting them: the log is what the feed serves,
    -- and erasing it would retroactively withdraw transitions that consumers have already been told
    -- about, and hide the removal itself.
    CONSTRAINT extension_version_change_extension_version_id_fkey
        FOREIGN KEY (extension_version_id) REFERENCES public.extension_version (id) ON DELETE SET NULL
);

-- The feed is ordered by the transition instant, with the id breaking ties between transitions that
-- share one, so that paging through the feed can neither skip nor repeat an entry. Expression and
-- ordering have to match the ones used by the query, so that the planner can serve the range scan,
-- the ordering and the count from this index alone.
CREATE INDEX extension_version_change_feed_idx ON public.extension_version_change (changed_at, id);

-- Backing the lookup of a single version's history, and the ON DELETE SET NULL above.
CREATE INDEX extension_version_change_extension_version_id_idx
    ON public.extension_version_change (extension_version_id);

-- Seed the log from the versions that are or were publicly available: their publication, plus their
-- removal for the ones that have been removed. Versions that are currently inactive without being
-- removed are deliberately left out, as there is no record of when they were withdrawn and they are
-- not public: they enter the feed on their next transition. Versions without a publication timestamp
-- are left out for the same reason.
INSERT INTO public.extension_version_change (
        extension_version_id, namespace, extension, version, target_platform, state, timestamp, changed_at)
SELECT ev.id, n.name, e.name, ev.version, ev.target_platform, 'ACTIVE', ev.timestamp, ev.timestamp
FROM public.extension_version ev
JOIN public.extension e ON e.id = ev.extension_id
JOIN public.namespace n ON n.id = e.namespace_id
WHERE (ev.active OR ev.removed) AND ev.timestamp IS NOT NULL;

-- Ordered after the publication of the same version: either its removal timestamp is later, or the
-- fallback makes the two share an instant and the higher id decides.
--
-- Gated on the publication timestamp, exactly as the insert above is, so that the two either both seed a
-- version or neither does. A removal on its own would report the withdrawal of a version whose
-- publication the feed never reported, which is the one thing the log is not allowed to say -- the same
-- rule the live path keeps when it purges a version it never reported, see ExtensionService#recordPurge.
INSERT INTO public.extension_version_change (
        extension_version_id, namespace, extension, version, target_platform, state, timestamp, changed_at)
SELECT ev.id, n.name, e.name, ev.version, ev.target_platform, 'REMOVED', ev.timestamp,
        -- a version removed before removal timestamps were recorded shares the instant of its
        -- publication, and the id above decides between the two
        COALESCE(ev.removed_timestamp, ev.timestamp)
FROM public.extension_version ev
JOIN public.extension e ON e.id = ev.extension_id
JOIN public.namespace n ON n.id = e.namespace_id
WHERE ev.removed AND ev.timestamp IS NOT NULL;
