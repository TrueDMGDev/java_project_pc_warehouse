ALTER TABLE stock_movements
    ADD COLUMN IF NOT EXISTS external_source VARCHAR(120),
    ADD COLUMN IF NOT EXISTS external_destination VARCHAR(120);

UPDATE stock_movements
SET external_source = NULLIF(BTRIM((regexp_match(details, 'Supplier:[[:space:]]*([^|]+)'))[1]), '')
WHERE movement_type = 'SUPPLY'
  AND details IS NOT NULL
  AND details ~ 'Supplier:[[:space:]]*[^|]+'
  AND external_source IS NULL;

UPDATE stock_movements
SET external_destination = NULLIF(BTRIM((regexp_match(details, 'Destination:[[:space:]]*([^|]+)'))[1]), '')
WHERE movement_type = 'DISPATCH'
  AND details IS NOT NULL
  AND details ~ 'Destination:[[:space:]]*[^|]+'
  AND external_destination IS NULL;

UPDATE stock_movements
SET details = NULLIF(
        BTRIM(
            regexp_replace(
                regexp_replace(
                    details,
                    '(^|[[:space:]]*\|[[:space:]]*)Supplier:[[:space:]]*[^|]*([[:space:]]*\|[[:space:]]*|$)',
                    '\1',
                    'g'
                ),
                '^[[:space:]]*\|[[:space:]]*|[[:space:]]*\|[[:space:]]*$',
                '',
                'g'
            )
        ),
        ''
    )
WHERE movement_type = 'SUPPLY'
  AND details IS NOT NULL
  AND details LIKE '%Supplier:%';

UPDATE stock_movements
SET details = NULLIF(
        BTRIM(
            regexp_replace(
                regexp_replace(
                    details,
                    '(^|[[:space:]]*\|[[:space:]]*)Destination:[[:space:]]*[^|]*([[:space:]]*\|[[:space:]]*|$)',
                    '\1',
                    'g'
                ),
                '^[[:space:]]*\|[[:space:]]*|[[:space:]]*\|[[:space:]]*$',
                '',
                'g'
            )
        ),
        ''
    )
WHERE movement_type = 'DISPATCH'
  AND details IS NOT NULL
  AND details LIKE '%Destination:%';
