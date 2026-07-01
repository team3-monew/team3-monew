package com.monew.server.interest.entity;

import com.monew.server.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "interests")
public class Interest extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // 성능 개선: 유사 이름 검색용 정규화 컬럼 추가
    @Column(name = "normalized_name", nullable = false, length = 50)
    private String normalizedName;

    @Column(name = "subscriber_count", nullable = false)
    private long subscriberCount = 0L;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Interest(String name) {
        this.name = name;
        this.normalizedName = normalizeName(name);
        this.subscriberCount = 0L;
        this.updatedAt = LocalDateTime.now();
    }

    // 성능 개선: 비교/검색용 이름 정규화
    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    public void increaseSubscriberCount() {
        this.subscriberCount++;
    }

    public void decreaseSubscriberCount() {
        if (this.subscriberCount <= 0) {
            this.subscriberCount = 0;
            return;
        }
        this.subscriberCount--;
    }

    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

}
