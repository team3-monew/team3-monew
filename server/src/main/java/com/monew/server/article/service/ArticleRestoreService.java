package com.monew.server.article.service;

import com.monew.server.article.dto.ArticleRestoreResultDto;
import java.time.LocalDateTime;

public interface ArticleRestoreService {

  ArticleRestoreResultDto restore(LocalDateTime from, LocalDateTime to);
}
