WITH ranked_active_versions AS (
    SELECT id,
           ROW_NUMBER() OVER (ORDER BY imported_at DESC, id DESC) AS rn
    FROM dataset_version
    WHERE is_active = TRUE
)
UPDATE dataset_version
SET is_active = FALSE
WHERE id IN (
    SELECT id
    FROM ranked_active_versions
    WHERE rn > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_dataset_version_single_active
    ON dataset_version (is_active)
    WHERE is_active = TRUE;
