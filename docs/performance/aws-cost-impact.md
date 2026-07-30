# AWS cost-impact and approval worksheet

Status date: 2026-07-29

Approval status: **no cost-producing change is approved by this document**

AWS prices, taxes, free-tier eligibility, discounts and exchange rates change. Obtain a current AWS Pricing Calculator quote for the selected region and a current Cost Explorer baseline immediately before approval. Do not use remembered or approximate prices as an approval basis.

## Last verified cost-bearing baseline (2026-07-28)

This baseline is from the detailed 2026-07-28 inventory. The 2026-07-29 refresh
could not authenticate because the default root login expired. Re-run Cost
Explorer and the complete inventory with a least-privilege SSO/assumed role
before using these resources in an approval packet.

- Staging ECS: two 1-vCPU/2-GiB Fargate tasks, scalable to six.
- Staging RDS: Multi-AZ `db.t4g.medium`, 50 GiB gp3 with autoscaling.
- Staging Valkey: two `cache.t4g.small` nodes.
- One CloudFront distribution, one staging ALB and private S3 buckets.
- CloudWatch logs, Container Insights and fourteen alarms.
- No SQS queues and no RDS Proxy.
- Two unrelated/standalone running EC2 instances also accrue cost and should be assigned an owner.

The former production VPC, RDS, ALB/TG and EC2 resources were deleted. Their absence is an infrastructure blocker, not a saving to assume in the target estimate.

The authoritative production proposal is ECS/Fargate behind an ALB, not the
legacy single-instance `infra/terraform-ec2` path. Local Terraform prepares
2–12 API tasks, a separately sized async worker, SQS/DLQs, private KMS-encrypted
upload/report storage, additional alarms/dashboard and optional GuardDuty
Malware Protection for S3. None of these changes is live. Async queues and
malware protection default to disabled in the production example.

## Candidate cost drivers

| Candidate               | Current comparison                                | Quote inputs required                                                                           | Approval note                                             |
| ----------------------- | ------------------------------------------------- | ----------------------------------------------------------------------------------------------- | --------------------------------------------------------- |
| ECS peak scaling        | Maximum six 1-vCPU/2-GiB tasks                    | Region, architecture, task CPU/memory, scheduled peak hours, on-demand/Spot mix                 | Quote 2, 6, 8 and 12-task hours separately                |
| Larger ECS task         | 1 vCPU/2 GiB to 2 vCPU/4 GiB                      | vCPU-hours and GiB-hours at measured task count                                                 | Approve only with throughput evidence                     |
| External RDS class      | Staging `db.t4g.medium`; candidate `db.r7g.large` | Exact production region, Multi-AZ mode, instance hours, storage, IOPS, backup and data transfer | RDS owner provides and applies quote/change               |
| Larger RDS candidate    | `db.r7g.xlarge`                                   | Same inputs plus test evidence that large is insufficient                                       | Do not include by default                                 |
| RDS Proxy               | None                                              | Underlying database vCPU-hours, endpoints and transfer                                          | Benchmark pinning before approval                         |
| Valkey capacity         | Two `cache.t4g.small` nodes                       | Node type/count, replicas, snapshot storage and transfer                                        | Resize only after memory/eviction evidence                |
| SQS email/report queues | None                                              | Requests, payload chunks, retention, DLQ and polling behavior                                   | Include producer and worker traffic                       |
| Worker compute          | None dedicated                                    | ECS task or approved compute size, count and duty cycle                                         | Keep independent from API tasks                           |
| S3 results/quarantine   | Existing private buckets                          | Stored GiB-month, PUT/GET/Lifecycle requests, result retention                                  | Include incomplete-upload cleanup                         |
| Malware scanning        | GuardDuty plan prepared locally; disabled         | Objects/GB scanned, S3 requests/tags, EventBridge and alert traffic                             | Requires quarantine consumer, quote and separate approval |
| CloudWatch              | Limited alarms; no dashboard                      | Custom metrics, dashboard, alarms, log ingest, retention and queries                            | Include load-test evidence retention                      |
| ALB/CloudFront logging  | Disabled                                          | Request volume, log delivery, S3 storage and query costs                                        | Required for useful production evidence                   |
| Load generation         | Not present                                       | Generator type/count/region, duration, data transfer and observability                          | Never run from one undersized generator                   |
| NAT/data transfer       | Architecture unresolved                           | NAT gateway hours/GB, cross-AZ and internet egress                                              | Model multi-AZ routing explicitly                         |

## Calculation formulas

Use current provider quotes with these workload variables:

```text
ECS monthly task-hours =
  off_peak_tasks × off_peak_hours
  + scheduled_peak_tasks × scheduled_peak_hours
  + autoscaled_extra_task_hours

Queue requests =
  sends + receives + deletes + visibility changes + DLQ operations
  multiplied by payload request chunks

Report storage GiB-month =
  average_result_size_GiB × reports_per_month × average_retention_fraction

Load-test compute =
  generator_instance_hour_price × generator_count × test_hours
  + cross-region/internet data transfer
```

Include at least normal, expected-peak and stress-test scenarios. Use measured request rate and response sizes after the 500-user test to refine the higher-stage estimate.

## Required approval packet

1. Cost Explorer baseline for the previous complete billing period and current month-to-date.
2. Current calculator export for the exact target region.
3. ECS comparison for 1-vCPU/2-GiB and 2-vCPU/4-GiB tasks.
4. External RDS owner quote for current, `db.r7g.large` and evidence-gated `db.r7g.xlarge`.
5. RDS Proxy and Valkey alternatives.
6. SQS, worker, S3, malware-scanning and CloudWatch assumptions.
7. Load-generator and data-transfer cost.
8. Budget threshold, alarm recipient and cost rollback trigger.
9. Expected monthly delta and maximum test-window spend.
10. Named approver and approval timestamp.

## Cost controls

- Tag all test resources with project, environment, owner and expiry.
- Put a hard end time on generators and scheduled scaling.
- Keep writes and report/file journeys sampled during early stages.
- Apply lifecycle expiry to synthetic report and upload objects.
- Alert on unexpected NAT, logging, queue and load-generator spend.
- Stop after each stage; do not pay for higher load when a lower gate fails.
- Keep `async_queues_enabled=false` and `malware_protection_enabled=false`
  until their exact plan, recipient, security behavior and cost are approved.
- Treat the two standalone EC2 instances as separate existing cost; do not
  count either as production ECS capacity.

No Terraform apply, resize, purchase or load-generation spend is authorized until the packet contains a current quote and explicit approval.
