package com.monew.server.interest.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInterestKeyword is a Querydsl query type for InterestKeyword
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInterestKeyword extends EntityPathBase<InterestKeyword> {

    private static final long serialVersionUID = -1695042998L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInterestKeyword interestKeyword = new QInterestKeyword("interestKeyword");

    public final com.monew.server.common.entity.QBaseCreatedEntity _super = new com.monew.server.common.entity.QBaseCreatedEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final QInterest interest;

    public final StringPath keyword = createString("keyword");

    public QInterestKeyword(String variable) {
        this(InterestKeyword.class, forVariable(variable), INITS);
    }

    public QInterestKeyword(Path<? extends InterestKeyword> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInterestKeyword(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInterestKeyword(PathMetadata metadata, PathInits inits) {
        this(InterestKeyword.class, metadata, inits);
    }

    public QInterestKeyword(Class<? extends InterestKeyword> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.interest = inits.isInitialized("interest") ? new QInterest(forProperty("interest")) : null;
    }

}

