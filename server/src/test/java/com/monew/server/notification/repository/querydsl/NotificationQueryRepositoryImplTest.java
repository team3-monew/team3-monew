package com.monew.server.notification.repository.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.notification.NotificationErrorCode;
import com.querydsl.core.types.dsl.BooleanExpression;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationQueryRepositoryImplTest {

    @Test
    @DisplayName("커서 조건 생성 성공 - cursor와 after가 없으면 조건을 생성하지 않는다")
    void cursorCondition_success_nullCursorAndAfter() {
        // given
        NotificationQueryRepositoryImpl repository = new NotificationQueryRepositoryImpl(null);

        // when
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "cursorCondition", null, null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("커서 조건 생성 성공 - cursor와 after가 있으면 조건을 생성한다")
    void cursorCondition_success_validCursorAndAfter() {
        // given
        NotificationQueryRepositoryImpl repository = new NotificationQueryRepositoryImpl(null);
        String cursor = UUID.randomUUID().toString();
        LocalDateTime after = LocalDateTime.of(2026, 7, 1, 10, 0);

        // when
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "cursorCondition", cursor, after);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("커서 파싱 성공 - UUID 문자열이면 UUID로 변환한다")
    void parseCursor_success_validUuid() {
        // given
        NotificationQueryRepositoryImpl repository = new NotificationQueryRepositoryImpl(null);
        UUID cursorId = UUID.randomUUID();

        // when
        UUID result = ReflectionTestUtils.invokeMethod(repository, "parseCursor", cursorId.toString());

        // then
        assertThat(result).isEqualTo(cursorId);
    }

    @Test
    @DisplayName("커서 파싱 실패 - UUID 문자열이 아니면 커서 예외가 발생한다")
    void parseCursor_fail_invalidUuid() {
        // given
        NotificationQueryRepositoryImpl repository = new NotificationQueryRepositoryImpl(null);

        // when
        // then
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(repository, "parseCursor", "invalid-cursor"))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_CURSOR);
    }
}
