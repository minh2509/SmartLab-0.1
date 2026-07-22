-- =========================================================
-- DATABASE SCHEMA: LAB MANAGEMENT SYSTEM
-- PostgreSQL
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- 0. COMMON FUNCTION: AUTO UPDATE updated_at
-- =========================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =========================================================
-- 1. LABS
-- =========================================================

CREATE TABLE labs (
                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                      name VARCHAR(255) NOT NULL,
                      code VARCHAR(100) NOT NULL UNIQUE,

                      description TEXT,
                      mission TEXT,
                      vision TEXT,

                      logo_file_id UUID,
                      cover_file_id UUID,

                      contact_email VARCHAR(255),
                      website_url VARCHAR(255),

                      status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                          CHECK (status IN ('ACTIVE', 'INACTIVE')),

                      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_labs_updated_at
    BEFORE UPDATE ON labs
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


-- =========================================================
-- 2. USERS / ROLES / PERMISSIONS
-- =========================================================

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,

                       username VARCHAR(100) NOT NULL,
                       email VARCHAR(255) NOT NULL,

                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255) NOT NULL,

                       avatar_file_id UUID,

                       account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                           CHECK (account_status IN ('ACTIVE', 'LOCKED', 'PENDING', 'DELETED')),

                       last_login_at TIMESTAMPTZ,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                       deleted_at TIMESTAMPTZ,
                       deleted_by UUID REFERENCES users(id) ON DELETE SET NULL,

                       CONSTRAINT uq_users_lab_username UNIQUE (lab_id, username),
                       CONSTRAINT uq_users_lab_email UNIQUE (lab_id, email)
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE TABLE roles (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       code VARCHAR(50) NOT NULL UNIQUE,
                       name VARCHAR(100) NOT NULL,
                       description TEXT,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE permissions (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                             code VARCHAR(100) NOT NULL UNIQUE,
                             name VARCHAR(150) NOT NULL,
                             module VARCHAR(100) NOT NULL,
                             description TEXT,

                             created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE user_roles (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,

                            assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
                            assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                            status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                                CHECK (status IN ('ACTIVE', 'INACTIVE')),

                            CONSTRAINT uq_user_roles UNIQUE (user_id, role_id)
);


CREATE TABLE role_permissions (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                  permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,

                                  CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id)
);


-- =========================================================
-- 3. FILES
-- =========================================================

CREATE TABLE files (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,

                       original_name VARCHAR(255) NOT NULL,
                       stored_name VARCHAR(255) NOT NULL,
                       storage_path TEXT NOT NULL,

                       mime_type VARCHAR(150),
                       file_size BIGINT NOT NULL DEFAULT 0 CHECK (file_size >= 0),
                       file_extension VARCHAR(50),

                       uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,

                       visibility VARCHAR(30) NOT NULL DEFAULT 'PRIVATE'
                           CHECK (visibility IN ('PUBLIC', 'LAB_INTERNAL', 'PROJECT_INTERNAL', 'PRIVATE')),

                       scan_status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                           CHECK (scan_status IN ('PENDING', 'SAFE', 'BLOCKED')),

                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       deleted_at TIMESTAMPTZ
);


ALTER TABLE users
    ADD CONSTRAINT fk_users_avatar_file
        FOREIGN KEY (avatar_file_id) REFERENCES files(id) ON DELETE SET NULL;


ALTER TABLE labs
    ADD CONSTRAINT fk_labs_logo_file
        FOREIGN KEY (logo_file_id) REFERENCES files(id) ON DELETE SET NULL;


ALTER TABLE labs
    ADD CONSTRAINT fk_labs_cover_file
        FOREIGN KEY (cover_file_id) REFERENCES files(id) ON DELETE SET NULL;


-- =========================================================
-- 4. MEMBER PROFILES / RESEARCH FIELDS
-- =========================================================

CREATE TABLE member_profiles (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

                                 student_code VARCHAR(100),
                                 phone VARCHAR(50),
                                 personal_email VARCHAR(255),

                                 bio TEXT,
                                 specialization VARCHAR(255),

                                 joined_at DATE,

                                 activity_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                                     CHECK (activity_status IN ('ACTIVE', 'INACTIVE', 'ALUMNI')),

                                 github_url VARCHAR(255),
                                 linkedin_url VARCHAR(255),
                                 portfolio_url VARCHAR(255),

                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_member_profiles_updated_at
    BEFORE UPDATE ON member_profiles
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE TABLE research_fields (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 code VARCHAR(50) NOT NULL UNIQUE,
                                 name VARCHAR(150) NOT NULL,
                                 description TEXT,

                                 status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                                     CHECK (status IN ('ACTIVE', 'INACTIVE')),

                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE member_research_fields (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                        member_profile_id UUID NOT NULL REFERENCES member_profiles(id) ON DELETE CASCADE,
                                        research_field_id UUID NOT NULL REFERENCES research_fields(id) ON DELETE CASCADE,

                                        CONSTRAINT uq_member_research_fields UNIQUE (member_profile_id, research_field_id)
);


-- =========================================================
-- 5. PROJECTS
-- =========================================================

CREATE TABLE projects (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                          lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,

                          code VARCHAR(100) NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          slug VARCHAR(255) NOT NULL,

                          short_description TEXT,
                          description TEXT,
                          objective TEXT,

                          project_type VARCHAR(30) NOT NULL
                              CHECK (project_type IN ('PRODUCTION', 'RESEARCH')),

                          visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC'
                              CHECK (visibility IN ('PUBLIC', 'LAB_INTERNAL', 'PROJECT_INTERNAL', 'PRIVATE')),

                          status VARCHAR(30) NOT NULL DEFAULT 'PROPOSED'
                              CHECK (status IN (
                                                'PROPOSED',
                                                'PREPARING',
                                                'IN_PROGRESS',
                                                'PAUSED',
                                                'COMPLETED',
                                                'CLOSED'
                                  )),

                          progress_percent INT NOT NULL DEFAULT 0
                              CHECK (progress_percent >= 0 AND progress_percent <= 100),

                          start_date DATE,
                          expected_end_date DATE,
                          actual_end_date DATE,

                          cover_file_id UUID REFERENCES files(id) ON DELETE SET NULL,

                          created_by UUID REFERENCES users(id) ON DELETE SET NULL,

                          created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                          deleted_at TIMESTAMPTZ,

                          CONSTRAINT uq_projects_lab_code UNIQUE (lab_id, code),
                          CONSTRAINT uq_projects_lab_slug UNIQUE (lab_id, slug)
);

CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE TABLE project_research_fields (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                         project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                         research_field_id UUID NOT NULL REFERENCES research_fields(id) ON DELETE CASCADE,

                                         CONSTRAINT uq_project_research_fields UNIQUE (project_id, research_field_id)
);


CREATE TABLE project_members (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                 project_role VARCHAR(30) NOT NULL DEFAULT 'PROJECT_MEMBER'
                                     CHECK (project_role IN ('PROJECT_LEADER', 'PROJECT_MEMBER')),

                                 member_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                                     CHECK (member_status IN ('ACTIVE', 'REMOVED', 'LEFT')),

                                 joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 left_at TIMESTAMPTZ,

                                 added_by UUID REFERENCES users(id) ON DELETE SET NULL,
                                 note TEXT,

                                 CONSTRAINT uq_project_members UNIQUE (project_id, user_id)
);


CREATE TABLE project_join_requests (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                       project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                       requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                       desired_position VARCHAR(255),
                                       reason TEXT,
                                       skills TEXT,
                                       experience TEXT,
                                       introduction TEXT,

                                       cv_file_id UUID REFERENCES files(id) ON DELETE SET NULL,

                                       status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                                           CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),

                                       reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
                                       reviewed_at TIMESTAMPTZ,
                                       rejection_reason TEXT,

                                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_project_join_requests_updated_at
    BEFORE UPDATE ON project_join_requests
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE UNIQUE INDEX uq_pending_project_join_request
    ON project_join_requests(project_id, requester_id)
    WHERE status = 'PENDING';


-- =========================================================
-- 6. TASKS
-- =========================================================

CREATE TABLE tasks (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,

                       title VARCHAR(255) NOT NULL,
                       description TEXT,

                       priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM'
                           CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),

                       status VARCHAR(30) NOT NULL DEFAULT 'TODO'
                           CHECK (status IN ('TODO', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED')),

                       start_date DATE,
                       due_date DATE,
                       completed_at TIMESTAMPTZ,

                       created_by UUID REFERENCES users(id) ON DELETE SET NULL,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       deleted_at TIMESTAMPTZ
);

CREATE TRIGGER trg_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE TABLE task_assignees (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
                                assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED'
                                    CHECK (status IN ('ASSIGNED', 'REMOVED')),

                                CONSTRAINT uq_task_assignees UNIQUE (task_id, user_id)
);


CREATE TABLE task_reports (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                              reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                              content TEXT,
                              result_summary TEXT,

                              status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED'
                                  CHECK (status IN ('SUBMITTED', 'REVIEWED', 'REJECTED')),

                              reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
                              reviewed_at TIMESTAMPTZ,
                              feedback TEXT,

                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_task_reports_updated_at
    BEFORE UPDATE ON task_reports
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE TABLE task_attachments (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                                  file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
                                  uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,

                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                  CONSTRAINT uq_task_attachments UNIQUE (task_id, file_id)
);


-- =========================================================
-- 7. MEMBER EVALUATIONS
-- =========================================================

CREATE TABLE evaluation_criteria (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     code VARCHAR(100) NOT NULL UNIQUE,
                                     name VARCHAR(255) NOT NULL,
                                     description TEXT,

                                     max_score INT NOT NULL DEFAULT 10 CHECK (max_score > 0),

                                     status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                                         CHECK (status IN ('ACTIVE', 'INACTIVE')),

                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE member_evaluations (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,

                                    member_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    evaluator_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                    evaluation_period VARCHAR(100),
                                    overall_score NUMERIC(5,2) CHECK (overall_score >= 0),

                                    comment TEXT,

                                    evaluated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                    CONSTRAINT uq_member_evaluation_period
                                        UNIQUE (project_id, member_id, evaluator_id, evaluation_period)
);

CREATE TRIGGER trg_member_evaluations_updated_at
    BEFORE UPDATE ON member_evaluations
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE TABLE member_evaluation_details (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                           evaluation_id UUID NOT NULL REFERENCES member_evaluations(id) ON DELETE CASCADE,
                                           criteria_id UUID NOT NULL REFERENCES evaluation_criteria(id) ON DELETE CASCADE,

                                           score NUMERIC(5,2) NOT NULL CHECK (score >= 0),
                                           comment TEXT,

                                           CONSTRAINT uq_member_evaluation_details UNIQUE (evaluation_id, criteria_id)
);


-- =========================================================
-- 8. POSTS / BLOG / MODERATION
-- =========================================================

CREATE TABLE post_categories (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 code VARCHAR(100) NOT NULL UNIQUE,
                                 name VARCHAR(150) NOT NULL,
                                 description TEXT,

                                 status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                                     CHECK (status IN ('ACTIVE', 'INACTIVE')),

                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE posts (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
                       project_id UUID REFERENCES projects(id) ON DELETE SET NULL,

                       category_id UUID REFERENCES post_categories(id) ON DELETE SET NULL,
                       author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                       title VARCHAR(255) NOT NULL,
                       slug VARCHAR(255) NOT NULL,

                       summary TEXT,
                       content TEXT,

                       cover_file_id UUID REFERENCES files(id) ON DELETE SET NULL,

                       content_type VARCHAR(50) NOT NULL
                           CHECK (content_type IN (
                                                   'NEWS',
                                                   'LAB_ANNOUNCEMENT',
                                                   'PROJECT_ANNOUNCEMENT',
                                                   'MEMBER_BLOG',
                                                   'EXPERIENCE_SHARING',
                                                   'ACADEMIC_POST',
                                                   'RESEARCH_RESULT',
                                                   'EVENT_CONTENT'
                               )),

                       visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC'
                           CHECK (visibility IN ('PUBLIC', 'LAB_INTERNAL', 'PROJECT_INTERNAL', 'PRIVATE')),

                       moderation_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
                           CHECK (moderation_status IN (
                                                        'DRAFT',
                                                        'PENDING_REVIEW',
                                                        'NEEDS_REVISION',
                                                        'APPROVED',
                                                        'PUBLISHED',
                                                        'REJECTED'
                               )),

                       published_at TIMESTAMPTZ,

                       reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
                       reviewed_at TIMESTAMPTZ,
                       review_note TEXT,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       deleted_at TIMESTAMPTZ,

                       CONSTRAINT uq_posts_lab_slug UNIQUE (lab_id, slug)
);

CREATE TRIGGER trg_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE TABLE post_moderation_logs (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,

                                      action VARCHAR(50) NOT NULL
                                          CHECK (action IN (
                                          'CREATE',
                                          'SUBMIT',
                                          'APPROVE',
                                          'REQUEST_REVISION',
                                          'REJECT',
                                          'PUBLISH',
                                          'UNPUBLISH'
                                          )),

    from_status VARCHAR(30),
    to_status VARCHAR(30),

    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,

    reason TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE post_attachments (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                                  file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
                                  uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,

                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                  CONSTRAINT uq_post_attachments UNIQUE (post_id, file_id)
);


-- =========================================================
-- 9. NOTIFICATIONS
-- =========================================================

CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                               lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,

                               title VARCHAR(255) NOT NULL,
                               message TEXT,

                               notification_type VARCHAR(80) NOT NULL,

                               related_type VARCHAR(80),
                               related_id UUID,
                               link_url VARCHAR(500),

                               created_by UUID REFERENCES users(id) ON DELETE SET NULL,

                               created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE notification_recipients (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                         notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
                                         recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                         read_at TIMESTAMPTZ,
                                         deleted_at TIMESTAMPTZ,

                                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                         CONSTRAINT uq_notification_recipients UNIQUE (notification_id, recipient_id)
);


-- =========================================================
-- 10. DOCUMENTS - FUTURE EXPANSION
-- =========================================================

CREATE TABLE documents (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                           lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
                           project_id UUID REFERENCES projects(id) ON DELETE SET NULL,

                           title VARCHAR(255) NOT NULL,
                           description TEXT,

                           document_type VARCHAR(50) NOT NULL
                               CHECK (document_type IN (
                                                        'RESEARCH_DOC',
                                                        'PROGRESS_REPORT',
                                                        'FINAL_REPORT',
                                                        'GUIDE',
                                                        'PAPER',
                                                        'MEETING_MINUTES',
                                                        'EVENT_DOCUMENT',
                                                        'TASK_DOCUMENT',
                                                        'LAB_DOCUMENT'
                                   )),

                           visibility VARCHAR(30) NOT NULL DEFAULT 'PRIVATE'
                               CHECK (visibility IN ('PUBLIC', 'LAB_INTERNAL', 'PROJECT_INTERNAL', 'PRIVATE')),

                           status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                               CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),

                           current_version_id UUID,

                           created_by UUID REFERENCES users(id) ON DELETE SET NULL,

                           created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                           deleted_at TIMESTAMPTZ
);

CREATE TRIGGER trg_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE TABLE document_versions (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                   document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,

                                   version_number INT NOT NULL CHECK (version_number > 0),

                                   file_id UUID NOT NULL REFERENCES files(id) ON DELETE RESTRICT,

                                   change_note TEXT,

                                   uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
                                   uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                   CONSTRAINT uq_document_versions UNIQUE (document_id, version_number)
);


ALTER TABLE documents
    ADD CONSTRAINT fk_documents_current_version
        FOREIGN KEY (current_version_id) REFERENCES document_versions(id) ON DELETE SET NULL;


CREATE TABLE document_access_rules (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                       document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,

                                       user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                       project_id UUID REFERENCES projects(id) ON DELETE CASCADE,

                                       access_level VARCHAR(30) NOT NULL
                                           CHECK (access_level IN ('VIEW', 'EDIT', 'MANAGE')),

                                       granted_by UUID REFERENCES users(id) ON DELETE SET NULL,
                                       granted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- =========================================================
-- 11. EVENTS / CALENDAR - FUTURE EXPANSION
-- =========================================================

CREATE TABLE events (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                        lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
                        project_id UUID REFERENCES projects(id) ON DELETE SET NULL,

                        title VARCHAR(255) NOT NULL,
                        description TEXT,

                        event_scope VARCHAR(30) NOT NULL DEFAULT 'LAB'
                            CHECK (event_scope IN ('LAB', 'PROJECT', 'PUBLIC')),

                        event_type VARCHAR(30) NOT NULL DEFAULT 'OTHER'
                            CHECK (event_type IN ('MEETING', 'WORKSHOP', 'SEMINAR', 'DEADLINE', 'OTHER')),

                        start_time TIMESTAMPTZ NOT NULL,
                        end_time TIMESTAMPTZ,

                        location VARCHAR(255),
                        online_url VARCHAR(500),

                        format VARCHAR(30) NOT NULL DEFAULT 'OFFLINE'
                            CHECK (format IN ('OFFLINE', 'ONLINE', 'HYBRID')),

                        organizer_id UUID REFERENCES users(id) ON DELETE SET NULL,

                        status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED'
                            CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED')),

                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


CREATE TABLE event_participants (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                    participant_status VARCHAR(30) NOT NULL DEFAULT 'INVITED'
                                        CHECK (participant_status IN (
                                                                      'INVITED',
                                                                      'REGISTERED',
                                                                      'ATTENDED',
                                                                      'ABSENT',
                                                                      'CANCELLED'
                                            )),

                                    registered_at TIMESTAMPTZ,
                                    attended_at TIMESTAMPTZ,

                                    CONSTRAINT uq_event_participants UNIQUE (event_id, user_id)
);


-- =========================================================
-- 12. SECURITY LOGS
-- =========================================================

CREATE TABLE audit_logs (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                            lab_id UUID REFERENCES labs(id) ON DELETE CASCADE,

                            actor_id UUID REFERENCES users(id) ON DELETE SET NULL,

                            action VARCHAR(100) NOT NULL,

                            entity_type VARCHAR(100) NOT NULL,
                            entity_id UUID,

                            old_value JSONB,
                            new_value JSONB,

                            ip_address VARCHAR(100),
                            user_agent TEXT,

                            created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE login_histories (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 user_id UUID REFERENCES users(id) ON DELETE SET NULL,

                                 login_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                 ip_address VARCHAR(100),
                                 user_agent TEXT,

                                 success BOOLEAN NOT NULL DEFAULT false,
                                 failure_reason TEXT
);


-- =========================================================
-- 13. INDEXES
-- =========================================================

CREATE INDEX idx_users_lab_id ON users(lab_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status ON users(account_status);

CREATE INDEX idx_files_lab_id ON files(lab_id);
CREATE INDEX idx_files_uploaded_by ON files(uploaded_by);

CREATE INDEX idx_member_profiles_user_id ON member_profiles(user_id);

CREATE INDEX idx_projects_lab_id ON projects(lab_id);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_visibility ON projects(visibility);
CREATE INDEX idx_projects_created_by ON projects(created_by);

CREATE INDEX idx_project_members_project_id ON project_members(project_id);
CREATE INDEX idx_project_members_user_id ON project_members(user_id);
CREATE INDEX idx_project_members_role ON project_members(project_role);

CREATE INDEX idx_project_join_requests_project_id ON project_join_requests(project_id);
CREATE INDEX idx_project_join_requests_requester_id ON project_join_requests(requester_id);
CREATE INDEX idx_project_join_requests_status ON project_join_requests(status);

CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);

CREATE INDEX idx_task_assignees_task_id ON task_assignees(task_id);
CREATE INDEX idx_task_assignees_user_id ON task_assignees(user_id);

CREATE INDEX idx_task_reports_task_id ON task_reports(task_id);
CREATE INDEX idx_task_reports_reporter_id ON task_reports(reporter_id);

CREATE INDEX idx_member_evaluations_project_id ON member_evaluations(project_id);
CREATE INDEX idx_member_evaluations_member_id ON member_evaluations(member_id);
CREATE INDEX idx_member_evaluations_evaluator_id ON member_evaluations(evaluator_id);

CREATE INDEX idx_posts_lab_id ON posts(lab_id);
CREATE INDEX idx_posts_project_id ON posts(project_id);
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_status ON posts(moderation_status);
CREATE INDEX idx_posts_visibility ON posts(visibility);
CREATE INDEX idx_posts_published_at ON posts(published_at);

CREATE INDEX idx_notifications_lab_id ON notifications(lab_id);
CREATE INDEX idx_notifications_related ON notifications(related_type, related_id);

CREATE INDEX idx_notification_recipients_recipient_id ON notification_recipients(recipient_id);
CREATE INDEX idx_notification_recipients_read_at ON notification_recipients(read_at);

CREATE INDEX idx_documents_lab_id ON documents(lab_id);
CREATE INDEX idx_documents_project_id ON documents(project_id);
CREATE INDEX idx_documents_visibility ON documents(visibility);

CREATE INDEX idx_events_lab_id ON events(lab_id);
CREATE INDEX idx_events_project_id ON events(project_id);
CREATE INDEX idx_events_start_time ON events(start_time);
CREATE INDEX idx_events_status ON events(status);

CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

CREATE INDEX idx_login_histories_user_id ON login_histories(user_id);
CREATE INDEX idx_login_histories_login_at ON login_histories(login_at);


-- =========================================================
-- 14. SEED DATA
-- =========================================================

INSERT INTO roles (code, name, description)
VALUES
    ('ADMIN', 'Admin', 'Quản trị toàn bộ hệ thống'),
    ('LEADER', 'Leader', 'Quản lý dự án trong phạm vi được phân công'),
    ('MEMBER', 'Member', 'Thành viên phòng Lab')
    ON CONFLICT (code) DO NOTHING;


INSERT INTO research_fields (code, name, description)
VALUES
    ('AI', 'Artificial Intelligence', 'Trí tuệ nhân tạo'),
    ('ROBOTICS', 'Robotics', 'Robot và hệ thống tự động'),
    ('SE', 'Software Engineering', 'Kỹ thuật phần mềm')
    ON CONFLICT (code) DO NOTHING;


INSERT INTO post_categories (code, name, description)
VALUES
    ('NEWS', 'Tin tức', 'Tin tức chung của phòng Lab'),
    ('LAB_ANNOUNCEMENT', 'Thông báo toàn Lab', 'Thông báo dành cho toàn bộ phòng Lab'),
    ('PROJECT_ANNOUNCEMENT', 'Thông báo dự án', 'Thông báo trong phạm vi dự án'),
    ('MEMBER_BLOG', 'Blog thành viên', 'Bài viết chia sẻ của thành viên'),
    ('EXPERIENCE_SHARING', 'Chia sẻ kinh nghiệm', 'Bài viết chia sẻ kinh nghiệm học tập, nghiên cứu'),
    ('ACADEMIC_POST', 'Bài viết học thuật', 'Bài viết mang tính học thuật'),
    ('RESEARCH_RESULT', 'Kết quả nghiên cứu', 'Công bố hoặc tổng hợp kết quả nghiên cứu'),
    ('EVENT_CONTENT', 'Nội dung sự kiện', 'Bài viết liên quan tới sự kiện')
    ON CONFLICT (code) DO NOTHING;


INSERT INTO evaluation_criteria (code, name, description, max_score)
VALUES
    ('TASK_COMPLETION', 'Mức độ hoàn thành nhiệm vụ', 'Đánh giá khả năng hoàn thành nhiệm vụ được giao', 10),
    ('WORK_QUALITY', 'Chất lượng công việc', 'Đánh giá chất lượng sản phẩm hoặc kết quả công việc', 10),
    ('RESPONSIBILITY', 'Tinh thần trách nhiệm', 'Đánh giá mức độ trách nhiệm với công việc', 10),
    ('TEAMWORK', 'Khả năng làm việc nhóm', 'Đánh giá khả năng phối hợp với thành viên khác', 10),
    ('RESEARCH_ABILITY', 'Khả năng nghiên cứu', 'Đánh giá năng lực tìm hiểu, phân tích và nghiên cứu', 10),
    ('PROACTIVENESS', 'Mức độ chủ động', 'Đánh giá khả năng chủ động trong công việc', 10)
    ON CONFLICT (code) DO NOTHING;


-- Tạo Lab mẫu
INSERT INTO labs (name, code, description, mission, vision, status)
VALUES (
           'Research Lab',
           'MAIN_LAB',
           'Phòng Lab nghiên cứu về AI, Robotics và Software Engineering.',
           'Xây dựng môi trường nghiên cứu, học tập và phát triển dự án thực tế.',
           'Trở thành phòng Lab có năng lực nghiên cứu và triển khai sản phẩm chất lượng.',
           'ACTIVE'
       )
    ON CONFLICT (code) DO NOTHING;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS joined_at TIMESTAMPTZ DEFAULT NOW();


ALTER TABLE users
    ADD COLUMN IF NOT EXISTS joined_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

