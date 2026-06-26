package com.monew.server.interest.entity;

import com.monew.server.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "interest_keywords", uniqueConstraints = @UniqueConstraint(columnNames = {
    "interest_id", "keyword"}))
public class InterestKeyword extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    @Column(nullable = false, length = 50)
    private String keyword;

    public InterestKeyword(Interest interest, String keyword) {
        this.interest = interest;
        this.keyword = keyword;
    }

}
