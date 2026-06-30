package com.monew.server.article.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QArticleInterestId is a Querydsl query type for ArticleInterestId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QArticleInterestId extends BeanPath<ArticleInterestId> {

    private static final long serialVersionUID = -263277962L;

    public static final QArticleInterestId articleInterestId = new QArticleInterestId("articleInterestId");

    public final ComparablePath<java.util.UUID> articleId = createComparable("articleId", java.util.UUID.class);

    public final ComparablePath<java.util.UUID> interestId = createComparable("interestId", java.util.UUID.class);

    public QArticleInterestId(String variable) {
        super(ArticleInterestId.class, forVariable(variable));
    }

    public QArticleInterestId(Path<? extends ArticleInterestId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QArticleInterestId(PathMetadata metadata) {
        super(ArticleInterestId.class, metadata);
    }

}

