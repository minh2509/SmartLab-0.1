CREATE INDEX idx_users_lab_status ON users(lab_id, account_status);

CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

CREATE INDEX idx_files_lab_created_at ON files(lab_id, created_at DESC);
CREATE INDEX idx_files_uploaded_by ON files(uploaded_by);

CREATE INDEX idx_member_research_fields_research_field_id
    ON member_research_fields(research_field_id);

CREATE INDEX idx_projects_lab_status ON projects(lab_id, status);
CREATE INDEX idx_projects_lab_visibility ON projects(lab_id, visibility);
CREATE INDEX idx_projects_created_by ON projects(created_by);

CREATE INDEX idx_project_research_fields_research_field_id
    ON project_research_fields(research_field_id);

CREATE INDEX idx_project_members_user_status ON project_members(user_id, member_status);
CREATE INDEX idx_project_members_project_role ON project_members(project_id, project_role);

CREATE INDEX idx_project_join_requests_project_status
    ON project_join_requests(project_id, status);
CREATE INDEX idx_project_join_requests_requester_status
    ON project_join_requests(requester_id, status);
CREATE INDEX idx_project_join_requests_reviewed_by ON project_join_requests(reviewed_by);

CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX idx_tasks_project_due_date ON tasks(project_id, due_date);
CREATE INDEX idx_tasks_created_by ON tasks(created_by);

CREATE INDEX idx_task_assignees_user_id ON task_assignees(user_id);
CREATE INDEX idx_task_assignees_assigned_by ON task_assignees(assigned_by);

CREATE INDEX idx_task_reports_task_created_at ON task_reports(task_id, created_at DESC);
CREATE INDEX idx_task_reports_reporter_id ON task_reports(reporter_id);
CREATE INDEX idx_task_reports_reviewed_by ON task_reports(reviewed_by);

CREATE INDEX idx_task_attachments_file_id ON task_attachments(file_id);
CREATE INDEX idx_task_attachments_uploaded_by ON task_attachments(uploaded_by);

CREATE INDEX idx_member_evaluations_member_id ON member_evaluations(member_id);
CREATE INDEX idx_member_evaluations_evaluator_id ON member_evaluations(evaluator_id);
CREATE INDEX idx_member_evaluation_details_criteria_id
    ON member_evaluation_details(criteria_id);

CREATE INDEX idx_posts_lab_moderation_status ON posts(lab_id, moderation_status);
CREATE INDEX idx_posts_lab_visibility_published_at
    ON posts(lab_id, visibility, published_at DESC);
CREATE INDEX idx_posts_project_id ON posts(project_id);
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_reviewed_by ON posts(reviewed_by);

CREATE INDEX idx_post_moderation_logs_post_created_at
    ON post_moderation_logs(post_id, created_at DESC);
CREATE INDEX idx_post_moderation_logs_actor_id ON post_moderation_logs(actor_id);

CREATE INDEX idx_post_attachments_file_id ON post_attachments(file_id);
CREATE INDEX idx_post_attachments_uploaded_by ON post_attachments(uploaded_by);

CREATE INDEX idx_notifications_lab_created_at ON notifications(lab_id, created_at DESC);
CREATE INDEX idx_notifications_created_by ON notifications(created_by);
CREATE INDEX idx_notifications_related ON notifications(related_type, related_id);

CREATE INDEX idx_notification_recipients_recipient_read_at
    ON notification_recipients(recipient_id, read_at);

CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity_lookup ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

CREATE INDEX idx_login_histories_user_login_at ON login_histories(user_id, login_at DESC);
