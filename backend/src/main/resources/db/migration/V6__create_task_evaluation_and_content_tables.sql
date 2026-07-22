CREATE TABLE tasks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    description text,
    priority varchar(30) NOT NULL DEFAULT 'MEDIUM',
    status varchar(30) NOT NULL DEFAULT 'TODO',
    start_date date,
    due_date date,
    completed_at timestamptz,
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_tasks_status CHECK (
        status IN ('TODO', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED')
    ),
    CONSTRAINT ck_tasks_due_date_order CHECK (
        due_date IS NULL OR start_date IS NULL OR due_date >= start_date
    )
);

CREATE TRIGGER trg_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE task_assignees (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_by uuid,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    status varchar(30) NOT NULL DEFAULT 'ASSIGNED',
    CONSTRAINT fk_task_assignees_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignees_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignees_assigned_by FOREIGN KEY (assigned_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_task_assignees UNIQUE (task_id, user_id),
    CONSTRAINT ck_task_assignees_status CHECK (status IN ('ASSIGNED', 'REMOVED'))
);

CREATE TABLE task_reports (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id uuid NOT NULL,
    reporter_id uuid,
    content text,
    result_summary text,
    status varchar(30) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by uuid,
    reviewed_at timestamptz,
    feedback text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_task_reports_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_task_reports_reviewed_by FOREIGN KEY (reviewed_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_task_reports_status CHECK (status IN ('SUBMITTED', 'REVIEWED', 'REJECTED'))
);

CREATE TRIGGER trg_task_reports_updated_at
    BEFORE UPDATE ON task_reports
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE task_attachments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id uuid NOT NULL,
    file_id uuid NOT NULL,
    uploaded_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_task_attachments_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_attachments_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE RESTRICT,
    CONSTRAINT fk_task_attachments_uploaded_by FOREIGN KEY (uploaded_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_task_attachments UNIQUE (task_id, file_id)
);

CREATE TABLE member_evaluations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL,
    member_id uuid NOT NULL,
    evaluator_id uuid,
    evaluation_period varchar(100) NOT NULL,
    overall_score numeric(5, 2),
    comment text,
    evaluated_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_member_evaluations_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_evaluations_member FOREIGN KEY (member_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_evaluations_evaluator FOREIGN KEY (evaluator_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_member_evaluation_period UNIQUE NULLS NOT DISTINCT (
        project_id,
        member_id,
        evaluator_id,
        evaluation_period
    ),
    CONSTRAINT ck_member_evaluations_overall_score CHECK (
        overall_score IS NULL OR overall_score >= 0
    )
);

CREATE TRIGGER trg_member_evaluations_updated_at
    BEFORE UPDATE ON member_evaluations
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE member_evaluation_details (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_id uuid NOT NULL,
    criteria_id uuid NOT NULL,
    score numeric(5, 2) NOT NULL,
    comment text,
    CONSTRAINT fk_member_evaluation_details_evaluation FOREIGN KEY (evaluation_id)
        REFERENCES member_evaluations(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_evaluation_details_criteria FOREIGN KEY (criteria_id)
        REFERENCES evaluation_criteria(id) ON DELETE CASCADE,
    CONSTRAINT uq_member_evaluation_details UNIQUE (evaluation_id, criteria_id),
    CONSTRAINT ck_member_evaluation_details_score CHECK (score >= 0)
);

CREATE TABLE posts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id uuid NOT NULL,
    project_id uuid,
    category_id uuid,
    author_id uuid,
    title varchar(255) NOT NULL,
    slug varchar(255) NOT NULL,
    summary text,
    content text,
    cover_file_id uuid,
    content_type varchar(50) NOT NULL,
    visibility varchar(30) NOT NULL DEFAULT 'PUBLIC',
    moderation_status varchar(30) NOT NULL DEFAULT 'DRAFT',
    published_at timestamptz,
    reviewed_by uuid,
    reviewed_at timestamptz,
    review_note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT fk_posts_lab FOREIGN KEY (lab_id) REFERENCES labs(id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES post_categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_cover_file FOREIGN KEY (cover_file_id) REFERENCES files(id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_posts_lab_slug UNIQUE (lab_id, slug),
    CONSTRAINT ck_posts_content_type CHECK (
        content_type IN (
            'NEWS',
            'LAB_ANNOUNCEMENT',
            'PROJECT_ANNOUNCEMENT',
            'MEMBER_BLOG',
            'EXPERIENCE_SHARING',
            'ACADEMIC_POST',
            'RESEARCH_RESULT',
            'EVENT_CONTENT'
        )
    ),
    CONSTRAINT ck_posts_visibility CHECK (
        visibility IN ('PUBLIC', 'LAB_INTERNAL', 'PROJECT_INTERNAL', 'PRIVATE')
    ),
    CONSTRAINT ck_posts_moderation_status CHECK (
        moderation_status IN (
            'DRAFT',
            'PENDING_REVIEW',
            'NEEDS_REVISION',
            'APPROVED',
            'PUBLISHED',
            'REJECTED'
        )
    )
);

CREATE TRIGGER trg_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE post_moderation_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id uuid NOT NULL,
    action varchar(50) NOT NULL,
    from_status varchar(30),
    to_status varchar(30),
    actor_id uuid,
    reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_post_moderation_logs_post FOREIGN KEY (post_id)
        REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_moderation_logs_actor FOREIGN KEY (actor_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_post_moderation_logs_action CHECK (
        action IN ('CREATE', 'SUBMIT', 'APPROVE', 'REQUEST_REVISION', 'REJECT', 'PUBLISH', 'UNPUBLISH')
    ),
    CONSTRAINT ck_post_moderation_logs_from_status CHECK (
        from_status IS NULL OR from_status IN (
            'DRAFT',
            'PENDING_REVIEW',
            'NEEDS_REVISION',
            'APPROVED',
            'PUBLISHED',
            'REJECTED'
        )
    ),
    CONSTRAINT ck_post_moderation_logs_to_status CHECK (
        to_status IS NULL OR to_status IN (
            'DRAFT',
            'PENDING_REVIEW',
            'NEEDS_REVISION',
            'APPROVED',
            'PUBLISHED',
            'REJECTED'
        )
    )
);

CREATE TABLE post_attachments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id uuid NOT NULL,
    file_id uuid NOT NULL,
    uploaded_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_post_attachments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_attachments_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE RESTRICT,
    CONSTRAINT fk_post_attachments_uploaded_by FOREIGN KEY (uploaded_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_post_attachments UNIQUE (post_id, file_id)
);
