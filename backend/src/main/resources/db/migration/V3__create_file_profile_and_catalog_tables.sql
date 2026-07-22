CREATE TABLE files (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id uuid NOT NULL,
    original_name varchar(255) NOT NULL,
    stored_name varchar(255) NOT NULL,
    storage_path text NOT NULL,
    mime_type varchar(150),
    file_size bigint NOT NULL DEFAULT 0,
    file_extension varchar(50),
    uploaded_by uuid,
    visibility varchar(30) NOT NULL DEFAULT 'PRIVATE',
    scan_status varchar(30) NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT fk_files_lab FOREIGN KEY (lab_id) REFERENCES labs(id) ON DELETE CASCADE,
    CONSTRAINT fk_files_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_files_file_size_non_negative CHECK (file_size >= 0),
    CONSTRAINT ck_files_visibility CHECK (
        visibility IN ('PUBLIC', 'LAB_INTERNAL', 'PROJECT_INTERNAL', 'PRIVATE')
    ),
    CONSTRAINT ck_files_scan_status CHECK (scan_status IN ('PENDING', 'SAFE', 'BLOCKED'))
);

CREATE TABLE member_profiles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    student_code varchar(100),
    phone varchar(50),
    personal_email varchar(255),
    bio text,
    specialization varchar(255),
    joined_at date,
    activity_status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    github_url varchar(255),
    linkedin_url varchar(255),
    portfolio_url varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_member_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_member_profiles_user UNIQUE (user_id),
    CONSTRAINT ck_member_profiles_activity_status CHECK (
        activity_status IN ('ACTIVE', 'INACTIVE', 'ALUMNI')
    )
);

CREATE TRIGGER trg_member_profiles_updated_at
    BEFORE UPDATE ON member_profiles
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE research_fields (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(50) NOT NULL,
    name varchar(150) NOT NULL,
    description text,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_research_fields_code UNIQUE (code),
    CONSTRAINT ck_research_fields_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE member_research_fields (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    member_profile_id uuid NOT NULL,
    research_field_id uuid NOT NULL,
    CONSTRAINT fk_member_research_fields_member_profile FOREIGN KEY (member_profile_id)
        REFERENCES member_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_research_fields_research_field FOREIGN KEY (research_field_id)
        REFERENCES research_fields(id) ON DELETE CASCADE,
    CONSTRAINT uq_member_research_fields UNIQUE (member_profile_id, research_field_id)
);

CREATE TABLE post_categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(100) NOT NULL,
    name varchar(150) NOT NULL,
    description text,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_post_categories_code UNIQUE (code),
    CONSTRAINT ck_post_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE evaluation_criteria (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(100) NOT NULL,
    name varchar(255) NOT NULL,
    description text,
    max_score int NOT NULL DEFAULT 10,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_evaluation_criteria_code UNIQUE (code),
    CONSTRAINT ck_evaluation_criteria_max_score_positive CHECK (max_score > 0),
    CONSTRAINT ck_evaluation_criteria_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
