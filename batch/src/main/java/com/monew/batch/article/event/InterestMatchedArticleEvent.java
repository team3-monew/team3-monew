package com.monew.batch.article.event;

import java.util.List;
import java.util.UUID;

public record InterestMatchedArticleEvent(
        List<InterestMatchData> interests
) {

    public record InterestMatchData(
            UUID articleId,
            UUID interestId,
            String interestName
    ) {
    }
}