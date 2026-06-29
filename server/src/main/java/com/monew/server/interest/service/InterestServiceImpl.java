package com.monew.server.interest.service;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.interest.InterestErrorCode;
import com.monew.server.interest.dto.InterestDto;
import com.monew.server.interest.dto.InterestRegisterRequest;
import com.monew.server.interest.dto.InterestUpdateRequest;
import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.entity.InterestKeyword;
import com.monew.server.interest.repository.InterestKeywordRepository;
import com.monew.server.interest.repository.InterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestServiceImpl implements InterestService {

    private static final double SIMILARITY_THRESHOLD = 0.8;

    private final InterestRepository interestRepository;
    private final InterestKeywordRepository interestKeywordRepository;

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

}
