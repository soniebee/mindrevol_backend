-- Liquibase changeset for initial roles
-- changeset: author:init-roles
-- Create roles table if not exists
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Insert default roles
INSERT INTO roles (name, description) VALUES ('USER', 'Regular user role')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) VALUES ('ADMIN', 'Administrator role')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) VALUES ('MODERATOR', 'Moderator role')
ON CONFLICT (name) DO NOTHING;

-- rollback: DELETE FROM roles WHERE name IN ('USER', 'ADMIN', 'MODERATOR');

