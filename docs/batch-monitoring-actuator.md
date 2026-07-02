# Batch Monitoring Actuator Metrics

Batch monitoring uses Spring Actuator and Micrometer custom metrics in the `batch` module.
Admin modules, monitoring tables, Prometheus, Grafana, and CloudWatch integration are not included in this scope.

## Common Job Metrics

All batch jobs expose common execution metrics with a `job` tag.

```text
GET /actuator/metrics/monew.batch.job.run.total
GET /actuator/metrics/monew.batch.job.success.total
GET /actuator/metrics/monew.batch.job.failure.total
GET /actuator/metrics/monew.batch.job.duration
GET /actuator/metrics/monew.batch.job.last_success_time
```

Job-specific examples:

```text
GET /actuator/metrics/monew.batch.job.run.total?tag=job:article_collect
GET /actuator/metrics/monew.batch.job.success.total?tag=job:article_backup
GET /actuator/metrics/monew.batch.job.failure.total?tag=job:user_cleanup
GET /actuator/metrics/monew.batch.job.duration?tag=job:article_collect
```

## Article Collect Metrics

The article collect job exposes last-run processing counts.

```text
GET /actuator/metrics/monew.batch.article.collect.last.staged.count
GET /actuator/metrics/monew.batch.article.collect.last.saved.count
GET /actuator/metrics/monew.batch.article.collect.last.duplicate_skipped.count
GET /actuator/metrics/monew.batch.article.collect.last.invalid_skipped.count
GET /actuator/metrics/monew.batch.article.collect.last.article_interest_linked.count
```

Source-specific examples:

```text
GET /actuator/metrics/monew.batch.article.collect.last.staged.count?tag=source:NAVER
GET /actuator/metrics/monew.batch.article.collect.last.saved.count?tag=source:NAVER
GET /actuator/metrics/monew.batch.article.collect.last.saved.count?tag=source:RSS_YONHAP
GET /actuator/metrics/monew.batch.article.collect.last.saved.count?tag=source:RSS_CHOSUN
GET /actuator/metrics/monew.batch.article.collect.last.saved.count?tag=source:RSS_HANKYUNG
```

Overall article collect totals are tagged as:

```text
source:ALL
```

## Article Backup Metrics

The article backup job exposes last-run processing counts.

```text
GET /actuator/metrics/monew.batch.article.backup.last.target.count
GET /actuator/metrics/monew.batch.article.backup.last.success.count
GET /actuator/metrics/monew.batch.article.backup.last.failure.count
GET /actuator/metrics/monew.batch.article.backup.last.file.count
GET /actuator/metrics/monew.batch.article.backup.last.file.size.bytes
GET /actuator/metrics/monew.batch.article.backup.last.failed
```

## Article Restore Metrics

The restore metrics component is prepared for future restore jobs.

```text
GET /actuator/metrics/monew.batch.article.restore.last.target.count
GET /actuator/metrics/monew.batch.article.restore.last.restored.count
GET /actuator/metrics/monew.batch.article.restore.last.failure.count
```

These restore metrics appear after a restore job records values through `ArticleRestoreMetrics`.
