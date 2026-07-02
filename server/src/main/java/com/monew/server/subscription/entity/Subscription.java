package com.monew.server.subscription.entity;

import com.monew.server.common.entity.BaseCreatedEntity;
import com.monew.server.interest.entity.Interest;
import com.monew.server.user.entity.User;
import jakarta.persistence.*;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "subscriptions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id",
    "interest_id"}))
public class Subscription extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    public Subscription(User user, Interest interest) {
        this.user = user;
        this.interest = interest;
    }
}
