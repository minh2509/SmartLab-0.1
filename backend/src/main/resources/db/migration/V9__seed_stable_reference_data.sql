INSERT INTO roles (code, name, description)
VALUES
    ('SUPER_ADMIN', 'Super Admin', 'Full system-level administrator role'),
    ('ADMIN', 'Admin', 'Lab administrator role'),
    ('LEADER', 'Leader', 'Project leader role'),
    ('MEMBER', 'Member', 'Lab member role')
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO research_fields (code, name, description)
VALUES
    ('AI', 'Artificial Intelligence', 'Artificial intelligence research field'),
    ('ROBOTICS', 'Robotics', 'Robotics and automation research field'),
    ('SE', 'Software Engineering', 'Software engineering research field')
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO post_categories (code, name, description)
VALUES
    ('NEWS', 'News', 'General lab news'),
    ('LAB_ANNOUNCEMENT', 'Lab Announcement', 'Announcement for the whole lab'),
    ('PROJECT_ANNOUNCEMENT', 'Project Announcement', 'Announcement scoped to a project'),
    ('MEMBER_BLOG', 'Member Blog', 'Blog post written by a lab member'),
    ('EXPERIENCE_SHARING', 'Experience Sharing', 'Learning or research experience article'),
    ('ACADEMIC_POST', 'Academic Post', 'Academic article or technical write-up'),
    ('RESEARCH_RESULT', 'Research Result', 'Research result or publication summary'),
    ('EVENT_CONTENT', 'Event Content', 'Content related to a lab or research event')
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO evaluation_criteria (code, name, description, max_score)
VALUES
    (
        'TASK_COMPLETION',
        'Task Completion',
        'Measures completion of assigned work',
        10
    ),
    (
        'WORK_QUALITY',
        'Work Quality',
        'Measures quality of submitted work or outcomes',
        10
    ),
    (
        'RESPONSIBILITY',
        'Responsibility',
        'Measures responsibility and follow-through',
        10
    ),
    (
        'TEAMWORK',
        'Teamwork',
        'Measures collaboration with other members',
        10
    ),
    (
        'RESEARCH_ABILITY',
        'Research Ability',
        'Measures research, analysis, and learning ability',
        10
    ),
    (
        'PROACTIVENESS',
        'Proactiveness',
        'Measures initiative and proactive contribution',
        10
    )
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    max_score = EXCLUDED.max_score;
