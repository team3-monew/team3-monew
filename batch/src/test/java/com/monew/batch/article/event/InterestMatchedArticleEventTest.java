package com.monew.batch.article.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InterestMatchedArticleEventTest {

    @Test
    @DisplayName("관심사 매칭 이벤트 생성 성공 - 관심사 매칭 데이터를 그대로 보관한다")
    void createInterestMatchedArticleEvent_success() {
        // Given
        UUID articleId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        InterestMatchedArticleEvent.InterestMatchData matchData =
                new InterestMatchedArticleEvent.InterestMatchData(articleId, interestId, "인공지능");

        // When
        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(List.of(matchData));

        // Then
        assertThat(event.interests()).hasSize(1);
        assertThat(event.interests().get(0).articleId()).isEqualTo(articleId);
        assertThat(event.interests().get(0).interestId()).isEqualTo(interestId);
        assertThat(event.interests().get(0).interestName()).isEqualTo("인공지능");
    }

    @Test
    @DisplayName("관심사 매칭 데이터 생성 성공 - articleId, interestId, interestName을 그대로 보관한다")
    void createInterestMatchData_success() {
        // Given
        UUID articleId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        // When
        InterestMatchedArticleEvent.InterestMatchData matchData =
                new InterestMatchedArticleEvent.InterestMatchData(articleId, interestId, "백엔드");

        // Then
        assertThat(matchData.articleId()).isEqualTo(articleId);
        assertThat(matchData.interestId()).isEqualTo(interestId);
        assertThat(matchData.interestName()).isEqualTo("백엔드");
    }
}