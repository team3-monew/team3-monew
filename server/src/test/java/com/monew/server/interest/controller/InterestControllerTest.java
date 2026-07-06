package com.monew.server.interest.controller;

import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.interest.dto.InterestDto;
import com.monew.server.interest.dto.InterestRegisterRequest;
import com.monew.server.interest.dto.InterestUpdateRequest;
import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.service.InterestService;
import com.monew.server.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterestController.class)
class InterestControllerTest extends ControllerTestSupport {

    private static final String AUTH_HEADER = "Monew-Request-User-ID";

    @MockitoBean
    private InterestService interestService;

    @Test
    @DisplayName("관심사를 등록하면 201 Created와 관심사 응답을 반환한다")
    void register_success() throws Exception {
        // given
        UUID interestId = UUID.randomUUID();

        InterestRegisterRequest request = new InterestRegisterRequest(
                "스포츠",
                List.of("축구", "야구")
        );

        Interest interest = new Interest("스포츠");
        setInterestId(interest, interestId);

        InterestDto response = InterestDto.of(
                interest,
                List.of("축구", "야구"),
                false
        );

        given(interestService.register(request))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/interests")
                        .header(AUTH_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("스포츠"))
                .andExpect(jsonPath("$.keywords[0]").value("축구"))
                .andExpect(jsonPath("$.keywords[1]").value("야구"))
                .andExpect(jsonPath("$.subscriberCount").value(0))
                .andExpect(jsonPath("$.subscribedByMe").value(false));

        verify(interestService).register(request);
    }

    @Test
    @DisplayName("관심사 키워드를 수정하면 200 OK와 수정된 관심사 응답을 반환한다")
    void update_success() throws Exception {
        // given
        UUID interestId = UUID.randomUUID();

        InterestUpdateRequest request = new InterestUpdateRequest(
                List.of("축구", "농구")
        );

        Interest interest = new Interest("스포츠");
        setInterestId(interest, interestId);

        InterestDto response = InterestDto.of(
                interest,
                List.of("축구", "농구"),
                false
        );

        given(interestService.update(interestId, request))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/interests/{interestId}", interestId)
                        .header(AUTH_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("스포츠"))
                .andExpect(jsonPath("$.keywords[0]").value("축구"))
                .andExpect(jsonPath("$.keywords[1]").value("농구"));

        verify(interestService).update(interestId, request);
    }

    @Test
    @DisplayName("관심사를 삭제하면 204 No Content를 반환한다")
    void delete_success() throws Exception {
        // given
        UUID interestId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/interests/{interestId}", interestId)
                        .header(AUTH_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());

        verify(interestService).delete(interestId);
    }

    @Test
    @DisplayName("관심사 목록을 조회하면 커서 페이지 응답을 반환한다")
    void getInterests_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        Interest interest = new Interest("스포츠");
        setInterestId(interest, interestId);

        InterestDto interestDto = InterestDto.of(
                interest,
                List.of("축구", "야구"),
                true
        );

        CursorPageResponse<InterestDto> response = new CursorPageResponse<>(
                List.of(interestDto),
                "스포츠|" + interestId,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                1,
                1L,
                false
        );

        given(interestService.getInterests(
                eq("스포"),
                eq("name"),
                eq("ASC"),
                isNull(),
                isNull(),
                eq(10),
                eq(userId)
        )).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/interests")
                        .header(AUTH_HEADER, userId.toString())
                        .param("keyword", "스포")
                        .param("orderBy", "name")
                        .param("direction", "ASC")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("스포츠"))
                .andExpect(jsonPath("$.content[0].keywords[0]").value("축구"))
                .andExpect(jsonPath("$.content[0].subscribedByMe").value(true))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(interestService).getInterests(
                "스포",
                "name",
                "ASC",
                null,
                null,
                10,
                userId
        );
    }

    private void setInterestId(Interest interest, UUID interestId) {
        ReflectionTestUtils.setField(interest, "id", interestId);
    }
}