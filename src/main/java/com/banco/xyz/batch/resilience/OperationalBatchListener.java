package com.banco.xyz.batch.resilience;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OperationalBatchListener implements JobExecutionListener, SkipListener<Object, Object> {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Inicio job={} executionId={}", jobExecution.getJobInstance().getJobName(), jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        long skipped = jobExecution.getStepExecutions().stream().mapToLong(step -> step.getSkipCount()).sum();
        log.info("Fin job={} executionId={} status={} read={} write={} skip={}", jobName,
                jobExecution.getId(), jobExecution.getStatus(), jobExecution.getStepExecutions().stream()
                        .mapToLong(step -> step.getReadCount()).sum(),
                jobExecution.getStepExecutions().stream().mapToLong(step -> step.getWriteCount()).sum(), skipped);
    }

    @Override
    public void onSkipInRead(Throwable throwable) {
        recordSkip("lectura", throwable);
    }

    @Override
    public void onSkipInProcess(Object item, Throwable throwable) {
        recordSkip("procesamiento", throwable);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable throwable) {
        recordSkip("escritura", throwable);
    }

    private void recordSkip(String phase, Throwable throwable) {
        log.warn("Registro omitido en {}: {}", phase, throwable.getMessage());
    }
}
