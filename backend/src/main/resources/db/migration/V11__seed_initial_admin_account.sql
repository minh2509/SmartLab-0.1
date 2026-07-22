INSERT INTO labs (
    code,
    name,
    description,
    mission,
    vision,
    contact_email,
    website_url,
    status
)
VALUES (
    'SMARTLAB',
    'Smart Lab',
    'Default SmartLab workspace for the initial administrator account',
    'Support focused research collaboration and lab operations',
    'A modern research lab management system',
    'admin@smart.lab',
    'https://smart.lab',
    'ACTIVE'
)
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    mission = EXCLUDED.mission,
    vision = EXCLUDED.vision,
    contact_email = EXCLUDED.contact_email,
    website_url = EXCLUDED.website_url,
    status = EXCLUDED.status;

INSERT INTO users (
    lab_id,
    username,
    email,
    password_hash,
    full_name,
    account_status
)
SELECT
    labs.id,
    'admin',
    'admin@smart.lab',
    '$2a$10$d7kQAwLf4KgqyRAueDTGc.gmv2ubaO6PidzJzDvMO8p3IfRjQi.wC',
    'SmartLab Admin',
    'ACTIVE'
FROM labs
WHERE labs.code = 'SMARTLAB'
ON CONFLICT (lab_id, email) DO UPDATE
SET
    username = EXCLUDED.username,
    full_name = EXCLUDED.full_name,
    account_status = CASE
        WHEN users.account_status = 'DELETED' THEN 'ACTIVE'
        ELSE users.account_status
    END,
    deleted_at = NULL,
    deleted_by = NULL;

INSERT INTO user_roles (
    user_id,
    role_id,
    status
)
SELECT
    users.id,
    roles.id,
    'ACTIVE'
FROM users
JOIN labs ON labs.id = users.lab_id
JOIN roles ON roles.code IN ('SUPER_ADMIN', 'ADMIN')
WHERE labs.code = 'SMARTLAB'
  AND users.email = 'admin@smart.lab'
ON CONFLICT (user_id, role_id) DO UPDATE
SET status = 'ACTIVE';
