package com.monew.server.article.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QArticleInterest is a Querydsl query type for ArticleInterest
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QArticleInterest extends EntityPathBase<ArticleInterest> {

    private static final long serialVersionUID = -322061317L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QArticleInterest articleInterest = new QArticleInterest("articleInterest");

    public final com.monew.server.common.entity.QBaseCreatedEntity _super = new com.monew.server.common.entity.QBaseCreatedEntity(this);

    public final QArticle article;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QArticleInterestId id;

    public final com.monew.server.interest.entity.QInterest interest;

    public QArticleInterest(String variable) {
        this(ArticleInterest.class, forVariable(variable), INITS);
    }

    public QArticleInterest(Path<? extends ArticleInterest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QArticleInterest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QArticleInterest(PathMetadata metadata, PathInits inits) {
        this(ArticleInterest.class, metadata, inits);
    }

    public QArticleInterest(Class<? extends ArticleInterest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.article = inits.isInitialized("article") ? new QArticle(forProperty("article")) : null;
        this.id = inits.isInitialized("id") ? new QArticleInterestId(forProperty("id")) : null;
        this.interest = inits.isInitialized("interest") ? new com.monew.server.interest.entity.QInterest(forProperty("interest")) : null;
    }

}

