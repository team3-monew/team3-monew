package com.monew.server.article.service;

import com.monew.server.article.dto.ArticleRestoreResultDto;
import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRestoreService {

  List<ArticleRestoreResultDto> restore(LocalDateTime from, LocalDateTime to);
}
