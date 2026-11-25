CREATE TABLE benutzer (
                        benutzer_id UUID PRIMARY KEY,
                        kennung VARCHAR(255) NOT NULL UNIQUE,
                        email VARCHAR(255) NOT NULL,
                        aktiv BOOLEAN NOT NULL,
                        keycloak_id VARCHAR(255) NOT NULL UNIQUE,
                        mitarbeiter_id UUID NOT NULL UNIQUE
);
