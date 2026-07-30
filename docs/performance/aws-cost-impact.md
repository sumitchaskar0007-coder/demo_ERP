# AWS cost-impact worksheet

No cost-producing change in this branch has been applied.

AWS prices vary by region and date. Generate a fresh AWS Pricing Calculator estimate for `ap-south-1` immediately before approval and attach it to the change request.

| Proposed item | Current | Candidate | Cost behavior |
|---|---:|---:|---|
| ECS baseline | 2 × 1 vCPU/2 GiB | unchanged off peak | No baseline task-size increase |
| ECS peak | maximum 6 tasks | scheduled 8, maximum 12 | Fargate charges per task-second for requested CPU/memory |
| Larger ECS task test | 1 vCPU/2 GiB | 2 vCPU/4 GiB | Approximately doubles compute/memory charge per task; disabled by default |
| RDS | Multi-AZ `db.t4g.medium` | test `db.r7g.large`, then xlarge only with evidence | Recurring instance-price increase; obtain exact Multi-AZ quote |
| RDS Proxy | none | optional | Per underlying DB vCPU-hour; may also incur PrivateLink endpoint charges |
| SQS | none | four standard queues | Request based; first one million requests/month may fall in the free tier, subject to current account terms |
| S3 results/quarantine | existing private bucket | additional stored objects and requests | Storage, request and scan-compute usage |
| CloudWatch | basic alarms | metrics, logs, dashboard and alarms | Usage-based metric/log ingestion and retention |

Required approval packet:

1. Current monthly Cost Explorer baseline.
2. Calculator estimate for off-peak and peak task-hours.
3. RDS Multi-AZ price comparison.
4. RDS Proxy estimate.
5. Expected SQS, CloudWatch and malware-scanning volume.
6. Budget alarm and rollback cost.

Do not infer approval from this worksheet.
