CREATE TABLE rolle (
                     rolle_id UUID PRIMARY KEY,
                     bezeichnung VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE benutzerrolle (
                             benutzer_id UUID NOT NULL,
                             rolle_id UUID NOT NULL,

                             PRIMARY KEY (benutzer_id, rolle_id),

                             FOREIGN KEY (benutzer_id)
                               REFERENCES benutzer (benutzer_id),

                             FOREIGN KEY (rolle_id)
                               REFERENCES rolle (rolle_id)
);
