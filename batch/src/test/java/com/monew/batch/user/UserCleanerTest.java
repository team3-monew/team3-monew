package com.monew.batch.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.mongodb.client.result.DeleteResult;
import com.monew.batch.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class UserCleanerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @InjectMocks
    private UserCleaner userCleaner;

    @Test
    @DisplayName("물리삭제 대상 있으면 - Mongo 활동내역 먼저 삭제 후 PG 물리삭제")
    void cleanup_deletesMongoThenPg() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        given(userRepository.findIdsByDeletedAtBefore(threshold)).willReturn(ids);
        given(mongoTemplate.remove(any(Query.class), eq("user_activities")))
                .willReturn(DeleteResult.acknowledged(2));

        userCleaner.cleanup(threshold);

        // Mongo 활동내역(user_activities) 삭제 + PG 물리삭제가 조회된 ID로 호출됨
        then(mongoTemplate).should().remove(any(Query.class), eq("user_activities"));
        then(userRepository).should().deleteAllByIdIn(ids);

        // Mongo가 PG보다 '먼저' 실행돼야 함 (PG 실패해도 유령 활동내역이 남지 않도록)
        InOrder inOrder = inOrder(mongoTemplate, userRepository);
        inOrder.verify(mongoTemplate).remove(any(Query.class), eq("user_activities"));
        inOrder.verify(userRepository).deleteAllByIdIn(anyList());
    }

    @Test
    @DisplayName("물리삭제 대상 없으면 - Mongo/PG 아무것도 삭제하지 않음")
    void cleanup_noTarget_deletesNothing() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        given(userRepository.findIdsByDeletedAtBefore(threshold)).willReturn(List.of());

        userCleaner.cleanup(threshold);

        then(mongoTemplate).should(never()).remove(any(Query.class), anyString());
        then(userRepository).should(never()).deleteAllByIdIn(anyList());
    }
}
