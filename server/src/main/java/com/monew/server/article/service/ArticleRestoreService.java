package com.monew.server.article.service;

import com.monew.server.article.dto.ArticleRestoreResultDto;
import java.time.Instant;
import java.util.List;

public interface ArticleRestoreService {

  List<ArticleRestoreResultDto> restore(Instant from, Instant to);
}
