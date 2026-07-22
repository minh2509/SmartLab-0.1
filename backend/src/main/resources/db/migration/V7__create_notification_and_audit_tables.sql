CREATE TABLE notifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    message text,
    notification_type varchar(80) NOT NULL,
    related_type varchar(80),
    related_id uuid,
    link_url varchar(500),
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_notifications_lab FOREIGN KEY (lab_id) REFERENCES labs(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_created_by FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE notification_recipients (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id uuid NOT NULL,
    recipient_id uuid NOT NULL,
    read_at timestamptz,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_notification_recipients_notification FOREIGN KEY (notification_id)
        REFERENCES notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_recipients_recipient FOREIGN KEY (recipient_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_notification_recipients UNIQUE (notification_id, recipient_id)
);

CREATE TABLE audit_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id uuid,
    actor_id uuid,
    action varchar(100) NOT NULL,
    entity_type varchar(100) NOT NULL,
    entity_id uuid,
    old_value jsonb,
    new_value jsonb,
    ip_address varchar(100),
    user_agent text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_logs_lab FOREIGN KEY (lab_id) REFERENCES labs(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE login_histories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid,
    login_at timestamptz NOT NULL DEFAULT now(),
    ip_address varchar(100),
    user_agent text,
    success boolean NOT NULL DEFAULT false,
    failure_reason text,
    CONSTRAINT fk_login_histories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
