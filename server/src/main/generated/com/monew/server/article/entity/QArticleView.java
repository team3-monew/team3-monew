package com.monew.server.article.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QArticleView is a Querydsl query type for ArticleView
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QArticleView extends EntityPathBase<ArticleView> {

    private static final long serialVersionUID = 782917622L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QArticleView articleView = new QArticleView("articleView");

    public final com.monew.server.common.entity.QBaseCreatedEntity _super = new com.monew.server.common.entity.QBaseCreatedEntity(this);

    public final QArticle article;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final com.monew.server.user.entity.QUser user;

    public QArticleView(String variable) {
        this(ArticleView.class, forVariable(variable), INITS);
    }

    public QArticleView(Path<? extends ArticleView> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QArticleView(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QArticleView(PathMetadata metadata, PathInits inits) {
        this(ArticleView.class, metadata, inits);
    }

    public QArticleView(Class<? extends ArticleView> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.article = inits.isInitialized("article") ? new QArticle(forProperty("article")) : null;
        this.user = inits.isInitialized("user") ? new com.monew.server.user.entity.QUser(forProperty("user")) : null;
    }

}

