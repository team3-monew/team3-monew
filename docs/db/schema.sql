CREATE TABLE users (
    id UUID NOT NULL,
    email VARCHAR(320) NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_deleted_at
ON users (deleted_at)
WHERE deleted_at IS NOT NULL;


CREATE TABLE interests (
    id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    subscriber_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_interests PRIMARY KEY (id),
    CONSTRAINT uk_interests_name UNIQUE (name),
    CONSTRAINT chk_interests_subscriber_count_non_negative
        CHECK (subscriber_count >= 0)
);

CREATE INDEX idx_interests_name_created_at_id
ON interests (name, created_at, id);

CREATE INDEX idx_interests_subscriber_count_created_at_id
ON interests (subscriber_count, created_at, id);


CREATE TABLE interest_keywords (
    id UUID NOT NULL,
    interest_id UUID NOT NULL,
    keyword VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_interest_keywords PRIMARY KEY (id),
    CONSTRAINT fk_interest_keywords_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE,
    CONSTRAINT uk_interest_keywords_interest_keyword UNIQUE (interest_id, keyword)
);

CREATE INDEX idx_interest_keywords_keyword
ON interest_keywords (keyword);


CREATE TABLE subscriptions (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    interest_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_subscriptions PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE,
    CONSTRAINT uk_subscriptions_user_interest UNIQUE (user_id, interest_id)
);

CREATE INDEX idx_subscriptions_interest_id
ON subscriptions (interest_id);


CREATE TABLE articles (
    id UUID NOT NULL,
    source VARCHAR(20) NOT NULL,
    source_url TEXT NOT NULL,
    title VARCHAR(500) NOT NULL,
    publish_date TIMESTAMP NOT NULL,
    summary TEXT NULL,
    comment_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_articles PRIMARY KEY (id),
    CONSTRAINT uk_articles_source_url UNIQUE (source_url),
    CONSTRAINT chk_articles_source
        CHECK (source IN ('NAVER', 'HANKYUNG', 'CHOSUN', 'YEONHAP')),
    CONSTRAINT chk_articles_comment_count_non_negative
        CHECK (comment_count >= 0),
    CONSTRAINT chk_articles_view_count_non_negative
        CHECK (view_count >= 0)
);

CREATE INDEX idx_articles_active_publish_date_id
ON articles (publish_date DESC, id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_articles_active_comment_count_id
ON articles (comment_count DESC, id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_articles_active_view_count_id
ON articles (view_count DESC, id)
WHERE deleted_at IS NULL;


CREATE TABLE article_interests (
    article_id UUID NOT NULL,
    interest_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_article_interests PRIMARY KEY (article_id, interest_id),
    CONSTRAINT fk_article_interests_article
        FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_article_interests_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE
);

CREATE INDEX idx_article_interests_interest_article
ON article_interests (interest_id, article_id);


CREATE TABLE article_views (
    id UUID NOT NULL,
    article_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_article_views PRIMARY KEY (id),
    CONSTRAINT fk_article_views_article
        FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_article_views_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_article_views_article_user UNIQUE (article_id, user_id)
);

CREATE INDEX idx_article_views_user_created_at
ON article_views (user_id, created_at DESC);


CREATE TABLE article_backups (
    id UUID NOT NULL,
    backup_date DATE NOT NULL,
    s3_bucket VARCHAR(255) NOT NULL,
    s3_object_key TEXT NOT NULL,
    article_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_article_backups PRIMARY KEY (id),
    CONSTRAINT uk_article_backups_backup_date UNIQUE (backup_date),
    CONSTRAINT chk_article_backups_status
        CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_article_backups_article_count_non_negative
        CHECK (article_count >= 0)
);

CREATE INDEX idx_article_backups_status_created_at
ON article_backups (status, created_at DESC);


CREATE TABLE comments (
    id UUID NOT NULL,
    article_id UUID NOT NULL,
    user_id UUID NOT NULL,
    content VARCHAR(500) NOT NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT pk_comments PRIMARY KEY (id),
    CONSTRAINT fk_comments_article
        FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_comments_content_length
        CHECK (char_length(content) BETWEEN 1 AND 500),
    CONSTRAINT chk_comments_like_count_non_negative
        CHECK (like_count >= 0)
);

CREATE INDEX idx_comments_article_created_at_id
ON comments (article_id, created_at DESC, id);

CREATE INDEX idx_comments_article_like_count_id
ON comments (article_id, like_count DESC, id);

CREATE INDEX idx_comments_user_id
ON comments (user_id);


CREATE TABLE comment_likes (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    comment_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_comment_likes PRIMARY KEY (id),
    CONSTRAINT fk_comment_likes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_likes_comment
        FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT uk_comment_likes_comment_user UNIQUE (comment_id, user_id)
);

CREATE INDEX idx_comment_likes_user_id_created_at
ON comment_likes (user_id, created_at DESC);


CREATE TABLE notifications (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    content VARCHAR(500) NOT NULL,
    resource_type VARCHAR(20) NOT NULL,
    resource_id UUID NOT NULL,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP NULL,

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_notifications_resource_type
        CHECK (resource_type IN ('INTEREST', 'COMMENT')),
    CONSTRAINT chk_notifications_confirmed_at
        CHECK (
            (confirmed = FALSE AND confirmed_at IS NULL)
            OR (confirmed = TRUE AND confirmed_at IS NOT NULL)
        )
);

CREATE INDEX idx_notifications_user_confirmed_created_at_id
ON notifications (user_id, confirmed, created_at DESC, id);

CREATE INDEX idx_notifications_confirmed_confirmed_at
ON notifications (confirmed, confirmed_at);
