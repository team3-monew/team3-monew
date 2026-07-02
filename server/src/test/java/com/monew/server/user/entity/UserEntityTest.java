package com.monew.server.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link User} 엔티티의 비즈니스 메서드 단위 테스트.
 *
 * <p>User 는 setter 대신 의미 있는 비즈니스 메서드로만 상태를 바꿈
 * 논리 삭제는 {@code deletedAt} 값 유무로 표현하며, {@code isDeleted()} 가 이를 판단
 */
class UserEntityTest {

    @Test
    @DisplayName("생성 직후에는 삭제 상태가 아니다")
    void notDeletedOnCreate() {
        User user = new User("woody@monew.com", "우디", "hashed-pw");

        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("updateNickname - 닉네임만 변경된다")
    void updateNickname() {
        User user = new User("woody@monew.com", "우디", "hashed-pw");

        user.updateNickname("뉴우디");

        assertThat(user.getNickname()).isEqualTo("뉴우디");
        assertThat(user.getEmail()).isEqualTo("woody@monew.com"); // 나머지는 불변
    }

    @Test
    @DisplayName("delete - 논리 삭제되면 deletedAt 이 채워지고 isDeleted 가 true")
    void delete() {
        User user = new User("woody@monew.com", "우디", "hashed-pw");

        user.delete();

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("restore - 논리 삭제를 되돌리면 다시 활성 상태")
    void restore() {
        User user = new User("woody@monew.com", "우디", "hashed-pw");
        user.delete();

        user.restore();

        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getDeletedAt()).isNull();
    }
}
