CREATE TABLE projects (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id uuid NOT NULL,
    code varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    slug varchar(255) NOT NULL,
    short_description text,
    description text,
    objective text,
    project_type varchar(30) NOT NULL,
    visibility varchar(30) NOT NULL DEFAULT 'PUBLIC',
    status varchar(30) NOT NULL DEFAULT 'PROPOSED',
    progress_percent int NOT NULL DEFAULT 0,
    start_date date,
    expected_end_date date,
    actual_end_date date,
    cover_file_id uuid,
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT fk_projects_lab FOREIGN KEY (lab_id) REFERENCES labs(id) ON DELETE CASCADE,
    CONSTRAINT fk_projects_cover_file FOREIGN KEY (cover_file_id) REFERENCES files(id) ON DELETE SET NULL,
    CONSTRAINT fk_projects_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_projects_lab_code UNIQUE (lab_id, code),
    CONSTRAINT uq_projects_lab_slug UNIQUE (lab_id, slug),
    CONSTRAINT ck_projects_project_type CHECK (project_type IN ('PRODUCTION', 'RESEARCH')),
    CONSTRAINT ck_projects_visibility CHECK (
        visibility IN ('PUBLIC', 'LAB_INTERNAL', 'PROJECT_INTERNAL', 'PRIVATE')
    ),
    CONSTRAINT ck_projects_status CHECK (
        status IN ('PROPOSED', 'PREPARING', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'CLOSED')
    ),
    CONSTRAINT ck_projects_progress_percent CHECK (
        progress_percent >= 0 AND progress_percent <= 100
    ),
    CONSTRAINT ck_projects_expected_end_date_order CHECK (
        expected_end_date IS NULL OR start_date IS NULL OR expected_end_date >= start_date
    ),
    CONSTRAINT ck_projects_actual_end_date_order CHECK (
        actual_end_date IS NULL OR start_date IS NULL OR actual_end_date >= start_date
    )
);

CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE project_research_fields (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL,
    research_field_id uuid NOT NULL,
    CONSTRAINT fk_project_research_fields_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_research_fields_research_field FOREIGN KEY (research_field_id)
        REFERENCES research_fields(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_research_fields UNIQUE (project_id, research_field_id)
);

CREATE TABLE project_members (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL,
    user_id uuid NOT NULL,
    project_role varchar(30) NOT NULL DEFAULT 'PROJECT_MEMBER',
    member_status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    joined_at timestamptz NOT NULL DEFAULT now(),
    left_at timestamptz,
    added_by uuid,
    note text,
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_added_by FOREIGN KEY (added_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_project_members UNIQUE (project_id, user_id),
    CONSTRAINT ck_project_members_project_role CHECK (
        project_role IN ('PROJECT_LEADER', 'PROJECT_MEMBER')
    ),
    CONSTRAINT ck_project_members_member_status CHECK (
        member_status IN ('ACTIVE', 'REMOVED', 'LEFT')
    )
);

CREATE TABLE project_join_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL,
    requester_id uuid NOT NULL,
    desired_position varchar(255),
    reason text,
    skills text,
    experience text,
    introduction text,
    cv_file_id uuid,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    reviewed_by uuid,
    reviewed_at timestamptz,
    rejection_reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_project_join_requests_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_join_requests_requester FOREIGN KEY (requester_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_join_requests_cv_file FOREIGN KEY (cv_file_id)
        REFERENCES files(id) ON DELETE SET NULL,
    CONSTRAINT fk_project_join_requests_reviewed_by FOREIGN KEY (reviewed_by)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_project_join_requests_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
    )
);

CREATE TRIGGER trg_project_join_requests_updated_at
    BEFORE UPDATE ON project_join_requests
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX uq_pending_project_join_request
    ON project_join_requests(project_id, requester_id)
    WHERE status = 'PENDING';
