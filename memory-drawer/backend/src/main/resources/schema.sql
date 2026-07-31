CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS memory_drafts (
    id CHAR(36) NOT NULL,
    owner_id CHAR(36) NOT NULL,
    original_image_key VARCHAR(512) NOT NULL,
    original_image_content_type VARCHAR(64) NOT NULL,
    parsed_content LONGTEXT NOT NULL,
    suggested_document_type VARCHAR(32),
    document_type VARCHAR(32),
    front_candidate LONGTEXT,
    draft_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);
