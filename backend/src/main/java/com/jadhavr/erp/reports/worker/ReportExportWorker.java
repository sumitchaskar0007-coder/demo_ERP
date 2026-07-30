package com.jadhavr.erp.reports.worker;

import com.jadhavr.erp.reports.config.ReportExportProperties;
import com.jadhavr.erp.reports.entity.ReportExportJob;
import com.jadhavr.erp.reports.service.ReportCsvExportService;
import com.jadhavr.erp.reports.service.ReportJobClaimService;
import com.jadhavr.erp.reports.transport.ReportJobQueue;
import com.jadhavr.erp.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ReportExportWorker {
    private static final Logger log = LoggerFactory.getLogger(ReportExportWorker.class);

    private final ReportExportProperties properties;
    private final ReportJobClaimService claims;
    private final ReportCsvExportService csv;
    private final ObjectStorageService storage;
    private final ReportJobQueue queue;
    private final Executor executor;
    private final AtomicBoolean sqsPollRunning = new AtomicBoolean();

    public ReportExportWorker(
            ReportExportProperties properties,
            ReportJobClaimService claims,
            ReportCsvExportService csv,
            ObjectStorageService storage,
            ReportJobQueue queue,
            @Qualifier("reportJobExecutor") Executor executor) {
        this.properties = properties;
        this.claims = claims;
        this.csv = csv;
        this.storage = storage;
        this.queue = queue;
        this.executor = executor;
    }

    @Scheduled(
            fixedDelayString = "${app.reports.database-poll-delay-ms:5000}",
            initialDelayString = "${app.reports.database-poll-initial-delay-ms:5000}")
    public void pollDatabase() {
        if (!properties.isEnabled() || !properties.isDatabasePollEnabled()) return;
        claims.recoverStale();
        for (ReportExportJob job : claims.claimBatch()) {
            executor.execute(() -> process(job));
        }
    }

    @Scheduled(
            fixedDelayString = "${app.reports.sqs.poll-delay-ms:1000}",
            initialDelayString = "${app.reports.sqs.poll-initial-delay-ms:5000}")
    public void pollSqs() {
        if (!properties.isEnabled()
                || !properties.getSqs().isConsumerEnabled()
                || !sqsPollRunning.compareAndSet(false, true)) {
            return;
        }
        executor.execute(() -> {
            try {
                for (ReportJobQueue.Delivery delivery : queue.receive()) {
                    claims.claim(delivery.jobId()).ifPresent(this::process);
                    queue.acknowledge(delivery);
                }
            } catch (RuntimeException exception) {
                log.warn("Report SQS poll failed; database fallback remains available");
            } finally {
                sqsPollRunning.set(false);
            }
        });
    }

    @Scheduled(
            fixedDelayString = "${app.reports.cleanup-delay-ms:300000}",
            initialDelayString = "${app.reports.cleanup-initial-delay-ms:60000}")
    public void cleanupExpired() {
        if (!properties.isEnabled()) return;
        for (ReportExportJob job : claims.expiredCompletedJobs()) {
            try {
                if (job.getResultKey() != null) storage.delete(job.getResultKey());
                claims.markExpired(job.getId());
            } catch (RuntimeException exception) {
                log.warn("Unable to clean expired report export {}", job.getId());
            }
        }
    }

    void process(ReportExportJob job) {
        String resultKey = resultKey(job);
        try {
            ReportCsvExportService.GeneratedReport report = csv.generate(
                    job.getReportType(),
                    job.getScopeCollegeId(),
                    job.getScopeDepartmentId(),
                    job.getStatusFilter());
            String filename = job.getReportType().pathValue() + "-report-" + job.getId() + ".csv";
            storage.put(resultKey, report.content(), ReportCsvExportService.CSV_CONTENT_TYPE);
            try {
                claims.complete(
                        job.getId(),
                        resultKey,
                        filename,
                        ReportCsvExportService.CSV_CONTENT_TYPE,
                        report.content().length);
            } catch (RuntimeException exception) {
                deleteQuietly(resultKey);
                throw exception;
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "Report export job {} attempt {} failed",
                    job.getId(),
                    job.getAttemptCount());
            claims.fail(job.getId());
        }
    }

    private String resultKey(ReportExportJob job) {
        return "colleges/" + job.getScopeCollegeId()
                + "/report-exports/" + job.getId()
                + "/" + job.getReportType().pathValue() + ".csv";
    }

    private void deleteQuietly(String resultKey) {
        try {
            storage.delete(resultKey);
        } catch (RuntimeException cleanupFailure) {
            log.warn("Unable to remove orphaned report object {}", resultKey);
        }
    }
}
