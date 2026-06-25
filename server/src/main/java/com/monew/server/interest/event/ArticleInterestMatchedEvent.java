package com.monew.server.interest.event;

import java.util.List;
import java.util.UUID;

public record ArticleInterestMatchedEvent(
        UUID articleId,
        String articleTitle,
        List<UUID> interestIds
) {
}