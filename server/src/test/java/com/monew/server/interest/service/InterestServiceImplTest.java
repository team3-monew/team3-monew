package com.monew.server.interest.service;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.interest.dto.InterestDto;
import com.monew.server.interest.dto.InterestRegisterRequest;
import com.monew.server.interest.dto.InterestUpdateRequest;
import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.entity.InterestKeyword;
import com.monew.server.interest.repository.InterestKeywordRepository;
import com.monew.server.interest.repository.InterestRepository;
import com.monew.server.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class InterestServiceImplTest {

    @InjectMocks
    private InterestServiceImpl interestService;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private InterestKeywordRepository interestKeywordRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Test
    @DisplayName("관심사를 등록하면 관심사와 키워드가 지정된다")
    void register_success() {
        // given
        InterestRegisterRequest request = new InterestRegisterRequest("스포츠", List.of(" 축구 ", "야구", "축구"));

        when(interestRepository.findSimilarNameCandidates("스포츠"))
                .thenReturn(List.of());

        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        InterestDto response = interestService.register(request);

        // then
        assertThat(response.name()).isEqualTo("스포츠");
        assertThat(response.keywords()).containsExactly("축구", "야구");
        assertThat(response.subscriberCount()).isZero();
        assertThat(response.subscribedByMe()).isFalse();

        verify(interestRepository).save(any(Interest.class));
        verify(interestKeywordRepository).saveAll(any(Collection.class));
    }

    @Test
    @DisplayName("유사한 관심사가 존재하면 등록에 실패한다")
    void register_similarInterestExists() {
        // given
        InterestRegisterRequest request = new InterestRegisterRequest("인공지능", List.of("AI"));

        when(interestRepository.findSimilarNameCandidates("인공지능"))
                .thenReturn(List.of("인공 지능"));

        // when & then
        assertThatThrownBy(() -> interestService.register(request))
                .isInstanceOf(BaseException.class);

        verify(interestRepository, never()).save(any(Interest.class));
        verify(interestKeywordRepository, never()).saveAll(any(Collection.class));
    }

    @Test
    @DisplayName("3글자 미만 관심사 이름은 전체 이름 조회 방식으로 유사도 검사를 수행한다")
    void register_shortNameFallback() {
        // given
        InterestRegisterRequest request = new InterestRegisterRequest("AI", List.of("인공지능"));

        when(interestRepository.findAllNames())
                .thenReturn(List.of());

        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        InterestDto response = interestService.register(request);

        // then
        assertThat(response.name()).isEqualTo("AI");
        assertThat(response.keywords()).containsExactly("인공지능");

        verify(interestRepository).findAllNames();
        verify(interestRepository, never()).findSimilarNameCandidates(any(String.class));
    }

    @Test
    @DisplayName("관심사 키워드를 수정할 수 있다")
    void update_success() {
        // given
        UUID interestId = UUID.randomUUID();
        Interest interest = new Interest("스포츠");

        InterestUpdateRequest request = new InterestUpdateRequest(List.of(" 축구 ", "야구", "축구"));

        when(interestRepository.findById(interestId))
                .thenReturn(Optional.of(interest));

        // when
        InterestDto response = interestService.update(interestId, request);

        // then
        assertThat(response.name()).isEqualTo("스포츠");
        assertThat(response.keywords()).containsExactly("축구", "야구");
        assertThat(interest.getUpdatedAt()).isNotNull();

        verify(interestKeywordRepository).deleteByInterestId(interestId);
        verify(interestKeywordRepository).saveAll(any(Collection.class));
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 수정하면 예외가 발생한다")
    void update_interestNotFound() {
        // given
        UUID interestId = UUID.randomUUID();
        InterestUpdateRequest request = new InterestUpdateRequest(List.of("축구"));

        when(interestRepository.findById(interestId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> interestService.update(interestId, request))
                .isInstanceOf(BaseException.class);

        verify(interestKeywordRepository, never()).deleteByInterestId(interestId);
        verify(interestKeywordRepository, never()).saveAll(any(Collection.class));
    }

    @Test
    @DisplayName("관심사를 삭제할 수 있다")
    void delete_success() {
        // given
        UUID interestId = UUID.randomUUID();
        Interest interest = new Interest("스포츠");

        when(interestRepository.findById(interestId))
                .thenReturn(Optional.of(interest));

        // when
        interestService.delete(interestId);

        // then
        verify(interestRepository).delete(interest);
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 삭제하면 예외가 발생한다")
    void delete_interestNotFound() {
        // given
        UUID interestId = UUID.randomUUID();

        when(interestRepository.findById(interestId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> interestService.delete(interestId))
                .isInstanceOf(BaseException.class);

        verify(interestRepository, never()).delete(any(Interest.class));
    }

    @Test
    @DisplayName("관심사 목록을 조회하면 키워드와 요청자의 구독 여부가 함께 반환된다")
    void getInterests_success() {
        // given
        UUID userId = UUID.randomUUID();

        UUID interestId1 = UUID.randomUUID();
        UUID interestId2 = UUID.randomUUID();
        UUID interestId3 = UUID.randomUUID();

        LocalDateTime createdAt1 = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime createdAt2 = LocalDateTime.of(2026, 7, 1, 11, 0);
        LocalDateTime createdAt3 = LocalDateTime.of(2026, 7, 1, 12, 0);

        Interest interest1 = new Interest("경제");
        Interest interest2 = new Interest("스포츠");
        Interest interest3 = new Interest("기술");

        setIdAndCreatedAt(interest1, interestId1, createdAt1);
        setIdAndCreatedAt(interest2, interestId2, createdAt2);
        setIdAndCreatedAt(interest3, interestId3, createdAt3);

        when(interestRepository.searchInterests(null, "name", "ASC",
                null, null, 3))
                .thenReturn(List.of(interest1, interest2, interest3));

        when(interestRepository.countInterests(null))
                .thenReturn(3L);

        when(interestKeywordRepository.findAllByInterestIdIn(List.of(interestId1, interestId2)))
                .thenReturn(List.of(
                        new InterestKeyword(interest1, "금융"),
                        new InterestKeyword(interest2, "축구"),
                        new InterestKeyword(interest2, "야구")
                ));

        when(subscriptionRepository.findSubscribedInterestIds(userId, List.of(interestId1, interestId2)))
                .thenReturn(List.of(interestId2));

        // when
        CursorPageResponse<InterestDto> response = interestService.getInterests(null, "name",
                "ASC", null, null, 2, userId);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3L);

        InterestDto first = response.content().get(0);
        InterestDto second = response.content().get(1);

        assertThat(first.name()).isEqualTo("경제");
        assertThat(first.keywords()).containsExactly("금융");
        assertThat(first.subscribedByMe()).isFalse();

        assertThat(second.name()).isEqualTo("스포츠");
        assertThat(second.keywords()).containsExactly("축구", "야구");
        assertThat(second.subscribedByMe()).isTrue();

        assertThat(response.nextCursor()).isEqualTo("스포츠|" + interestId2);
        assertThat(response.nextAfter()).isEqualTo(createdAt2);
    }

    @Test
    @DisplayName("관심사 목록 조회 결과가 비어 있으면 빈 페이지를 반환한다")
    void getInterests_empty() {
        // given
        UUID userId = UUID.randomUUID();

        when(interestRepository.searchInterests(null, "name", "ASC",
                null, null, 11)).thenReturn(List.of());

        when(interestRepository.countInterests(null))
                .thenReturn(0L);

        // when
        CursorPageResponse<InterestDto> response = interestService.getInterests(null, "name",
                "ASC", null, null, 10, userId);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.nextAfter()).isNull();
        assertThat(response.size()).isZero();
        assertThat(response.totalElements()).isZero();
        assertThat(response.hasNext()).isFalse();

        verify(interestKeywordRepository, never()).findAllByInterestIdIn(any(Collection.class));
        verify(subscriptionRepository, never()).findSubscribedInterestIds(any(UUID.class), any(Collection.class));
    }

    @Test
    @DisplayName("지원하지 않는 정렬 기준이면 예외가 발생한다")
    void getInterests_invalidOrderBy() {
        // when & then
        assertThatThrownBy(() -> interestService.getInterests(null, "createdAt", "ASC",
                null, null, 10, UUID.randomUUID()))
                .isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("지원하지 않는 정렬 방향이면 예외가 발생한다")
    void getInterests_invalidDirection() {
        // when & then
        assertThatThrownBy(() -> interestService.getInterests(null, "name", "DOWN",
                null, null, 10, UUID.randomUUID()))
                .isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("페이지 크기가 0 이하이면 예외가 발생한다")
    void getInterests_invalidLimit() {
        // when & then
        assertThatThrownBy(() -> interestService.getInterests(null, "name", "ASC",
                null, null, 0, UUID.randomUUID()))
                .isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("cursor와 after 중 하나만 있으면 예외가 발생한다")
    void getInterests_cursorWithoutAfter() {
        // when & then
        assertThatThrownBy(() -> interestService.getInterests(null, "name", "ASC",
                "스포츠|" + UUID.randomUUID(), null, 10, UUID.randomUUID()))
                .isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("subscriberCount 정렬에서 cursor 값이 숫자가 아니면 예외가 발생한다")
    void getInterests_invalidSubscriberCountCursor() {
        // when & then
        assertThatThrownBy(() -> interestService.getInterests(null, "subscriberCount", "ASC",
                "abc|" + UUID.randomUUID(), LocalDateTime.now(), 10, UUID.randomUUID()))
                .isInstanceOf(BaseException.class);
    }

    private void setIdAndCreatedAt(Interest interest, UUID id, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(interest, "id", id);
        ReflectionTestUtils.setField(interest, "createdAt", createdAt);
    }

}
