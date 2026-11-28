#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Ein vorhandener Realm behält beim erneuten Import seine ursprünglichen Benutzer-IDs.
ids=$(docker compose exec -T keycloak sh -eu -c '
    config=$(mktemp)
    trap '\''rm -f "$config"'\'' EXIT
    cli=/opt/keycloak/bin/kcadm.sh
    "$cli" config credentials --config "$config" --server http://localhost:8080 \
        --realm master --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" >&2
    for name in test-app-admin test-user; do
        "$cli" get users --config "$config" -r arbeitszeiterfassung \
            -q "username=$name" -q exact=true --fields id --format csv --noquotes
    done
')
uuid='[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}'
if [[ ! $ids =~ ^($uuid)$'\n'($uuid)$ ]]; then
    echo "Beide lokalen Testidentitäten müssen im Projektrealm existieren." >&2
    exit 1
fi
admin_id=${BASH_REMATCH[1]}
user_id=${BASH_REMATCH[2]}
docker compose exec -T app-db sh -c 'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$@"' \
    sh -v admin_id="$admin_id" -v user_id="$user_id" <<'SQL'
BEGIN;
CREATE TEMP TABLE testkonto (
    id uuid PRIMARY KEY, mitarbeiter uuid, kennung text, subject uuid, rolle text
) ON COMMIT DROP;
INSERT INTO testkonto VALUES
    ('cbf19448-7194-4cbe-a06d-b2ec8bad4e21', '5345a8f8-44d8-409b-8caa-ba7ae2d8bf16', 'test-app-admin', :'admin_id', 'ADMINISTRATION'),
    ('a4721b29-5db8-462f-b361-100bb404ca18', '32aacc3c-2831-4d52-840e-b9eb7192598e', 'test-user', :'user_id', 'MITARBEITER');
INSERT INTO benutzer (benutzer_id, kennung, email, aktiv, keycloak_id, mitarbeiter_id)
SELECT id, kennung, kennung || '@example.test', true, subject::text, mitarbeiter FROM testkonto
ON CONFLICT (benutzer_id) DO UPDATE SET keycloak_id = excluded.keycloak_id, aktiv = true;

DELETE FROM benutzerrolle WHERE benutzer_id IN (SELECT id FROM testkonto);
INSERT INTO benutzerrolle (benutzer_id, rolle_id)
SELECT id, (SELECT rolle_id FROM rolle WHERE bezeichnung = t.rolle) FROM testkonto t;
COMMIT;
SQL
