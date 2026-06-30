package com.monew.server.interest.service;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.interest.InterestErrorCode;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.interest.dto.InterestDto;
import com.monew.server.interest.dto.InterestRegisterRequest;
import com.monew.server.interest.dto.InterestUpdateRequest;
import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.entity.InterestKeyword;
import com.monew.server.interest.repository.InterestKeywordRepository;
import com.monew.server.interest.repository.InterestRepository;
import com.monew.server.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestServiceImpl implements InterestService {

    private static final double SIMILARITY_THRESHOLD = 0.8;

    private final InterestRepository interestRepository;
    private final InterestKeywordRepository interestKeywordRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public InterestDto register(InterestRegisterRequest request) {
        String name = request.name().trim();
        List<String> keywords = normalizeKeywords(request.keywords());

        validateSimilarInterestName(name);

        Interest interest = new Interest(name);
        Interest savedInterest = interestRepository.save(interest);

        List<InterestKeyword> interestKeywords = keywords.stream()
                .map(keyword -> new InterestKeyword(savedInterest, keyword))
                .toList();

        interestKeywordRepository.saveAll(interestKeywords);

        return InterestDto.of(savedInterest, keywords, false);
    }

    @Override
    @Transactional
    public InterestDto update(UUID interestId, InterestUpdateRequest request) {
        Interest interest = interestRepository.findById(interestId)
                .orElseThrow(() -> new BaseException(InterestErrorCode.INTEREST_NOT_FOUND));

        List<String> keywords = normalizeKeywords(request.keywords());

        interestKeywordRepository.deleteByInterest(interest);

        List<InterestKeyword> newKeywords = keywords.stream()
                .map(keyword -> new InterestKeyword(interest, keyword))
                .toList();

        interestKeywordRepository.saveAll(newKeywords);
        interest.markUpdated();

        return InterestDto.of(interest, keywords, false);
    }

    @Override
    @Transactional
    public void delete(UUID interestId) {
        Interest interest = interestRepository.findById(interestId)
                .orElseThrow(() -> new BaseException(InterestErrorCode.INTEREST_NOT_FOUND));
        interestRepository.delete(interest);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<InterestDto> getInterests(String keyword, String orderBy, String direction, String cursor,
                                                        LocalDateTime after, int limit, UUID requestUserId) {
        validateSearchCondition(orderBy, direction, cursor, after, limit);

        int fetchLimit = limit + 1;

        List<Interest> fetchedInterests = interestRepository.searchInterests(keyword, orderBy, direction, cursor, after,
                fetchLimit);
        boolean hasNext = fetchedInterests.size() > limit;

        List<Interest> interests = hasNext
                ? fetchedInterests.subList(0, limit) : fetchedInterests;
        long totalElements = interestRepository.countInterests(keyword);

        if (interests.isEmpty()) {
            return new CursorPageResponse<>(List.of(), null, null, 0, totalElements, false);
        }

        List<UUID> interestIds = interests.stream()
                .map(Interest::getId)
                .toList();
        Map<UUID, List<String>> keywordMap = interestKeywordRepository
                .findAllByInterestIdIn(interestIds).stream()
                .collect(Collectors.groupingBy(
                        interestKeyword -> interestKeyword.getInterest().getId(),
                        Collectors.mapping(InterestKeyword::getKeyword, Collectors.toList())
                ));
        Set<UUID> subscribedInterestIds = new HashSet<>(
                subscriptionRepository.findSubscribedInterestIds(requestUserId, interestIds)
        );

        List<InterestDto> content = interests.stream()
                .map(interest -> InterestDto.of(
                        interest,
                        keywordMap.getOrDefault(interest.getId(), List.of()),
                        subscribedInterestIds.contains(interest.getId())
                )).toList();
        Interest lastInterest = interests.get(interests.size() - 1);
        String nextCursor = hasNext ? getNextCursor(lastInterest, orderBy) : null;
        LocalDateTime nextAfter = hasNext ? lastInterest.getCreatedAt() : null;

        return new CursorPageResponse<>(
                content,
                nextCursor,
                nextAfter,
                content.size(),
                totalElements,
                hasNext
        );
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        Set<String> normalizedKeywords = new LinkedHashSet<>();

        for (String keyword : keywords) {
            normalizedKeywords.add(keyword.trim());
        }

        return normalizedKeywords.stream().toList();
    }

    private void validateSimilarInterestName(String name) {
        List<String> existingNames = interestRepository.findAllNames();

        for (String existingName : existingNames) {
            double similarity = calculateSimilarity(name, existingName);

            if (similarity >= SIMILARITY_THRESHOLD) {
                throw new BaseException(InterestErrorCode.SIMILAR_INTEREST_EXISTS);
            }
        }
    }

    private double calculateSimilarity(String source, String target) {
        String normalizedSource = source.toLowerCase();
        String normalizedTarget = target.toLowerCase();

        int maxLength = Math.max(normalizedSource.length(), normalizedTarget.length());

        if (maxLength == 0) {
            return 1.0;
        }

        int distance = calculateLevenshteinDistance(normalizedSource, normalizedTarget);
        return 1.0 - ((double) distance / maxLength);
    }

    private int calculateLevenshteinDistance(String source, String target) {
        int sourceLength = source.length();
        int targetLength = target.length();

        int[][] dp = new int[sourceLength + 1][targetLength + 1];

        for (int i = 0; i <= sourceLength; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= targetLength; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= sourceLength; i++) {
            for (int j = 1; j <= targetLength; j++) {
                int cost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[sourceLength][targetLength];
    }

    private void validateSearchCondition(String orderBy, String direction, String cursor,
                                         LocalDateTime after, int limit) {
        if (!orderBy.equals("name") && !orderBy.equals("subscriberCount")) {
            throw new BaseException(InterestErrorCode.INVALID_INTEREST_ORDER_BY);
        }

        if (!direction.equals("ASC") && !direction.equals("DESC")) {
            throw new BaseException(InterestErrorCode.INVALID_INTEREST_DIRECTION);
        }

        if (limit <= 0) {
            throw new BaseException(InterestErrorCode.INVALID_INTEREST_LIMIT);
        }

        if ((cursor == null && after != null) || (cursor != null && after == null)) {
            throw new BaseException(InterestErrorCode.INVALID_INTEREST_CURSOR);
        }

        if (cursor != null && orderBy.equals("subscriberCount")) {
            try {
                Long.parseLong(cursor);
            } catch (NumberFormatException e) {
                throw new BaseException(InterestErrorCode.INVALID_INTEREST_CURSOR);
            }
        }
    }

    private String getNextCursor(Interest interest, String orderBy) {
        if (orderBy.equals("name")) {
            return interest.getName();
        }
        return String.valueOf(interest.getSubscriberCount());
    }

}
