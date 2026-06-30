package com.monew.server.article.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QArticleBackup is a Querydsl query type for ArticleBackup
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QArticleBackup extends EntityPathBase<ArticleBackup> {

    private static final long serialVersionUID = 184519379L;

    public static final QArticleBackup articleBackup = new QArticleBackup("articleBackup");

    public final com.monew.server.common.entity.QBaseCreatedEntity _super = new com.monew.server.common.entity.QBaseCreatedEntity(this);

    public final NumberPath<Long> articleCount = createNumber("articleCount", Long.class);

    public final DatePath<java.time.LocalDate> backupDate = createDate("backupDate", java.time.LocalDate.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final StringPath s3Bucket = createString("s3Bucket");

    public final StringPath s3ObjectKey = createString("s3ObjectKey");

    public final EnumPath<ArticleBackupStatus> status = createEnum("status", ArticleBackupStatus.class);

    public QArticleBackup(String variable) {
        super(ArticleBackup.class, forVariable(variable));
    }

    public QArticleBackup(Path<? extends ArticleBackup> path) {
        super(path.getType(), path.getMetadata());
    }

    public QArticleBackup(PathMetadata metadata) {
        super(ArticleBackup.class, metadata);
    }

}

