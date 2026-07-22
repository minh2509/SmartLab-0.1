ALTER TABLE users
    ADD CONSTRAINT fk_users_avatar_file
    FOREIGN KEY (avatar_file_id) REFERENCES files(id) ON DELETE SET NULL;

ALTER TABLE labs
    ADD CONSTRAINT fk_labs_logo_file
    FOREIGN KEY (logo_file_id) REFERENCES files(id) ON DELETE SET NULL;

ALTER TABLE labs
    ADD CONSTRAINT fk_labs_cover_file
    FOREIGN KEY (cover_file_id) REFERENCES files(id) ON DELETE SET NULL;
