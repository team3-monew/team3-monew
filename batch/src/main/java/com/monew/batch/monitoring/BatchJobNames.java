package com.monew.batch.monitoring;

import java.util.Locale;

public final class BatchJobNames {

  private BatchJobNames() {
  }

  public static String normalize(String jobName) {
    if (jobName == null || jobName.isBlank()) {
      return "unknown";
    }

    String normalized = jobName;
    if (normalized.endsWith("Job")) {
      normalized = normalized.substring(0, normalized.length() - 3);
    }

    // Actuator tag 값은 외부에서 조회하기 쉽도록 camelCase Bean 이름을 snake_case로 통일합니다.
    return normalized
        .replaceAll("([a-z])([A-Z])", "$1_$2")
        .replace('-', '_')
        .toLowerCase(Locale.ROOT);
  }
}
