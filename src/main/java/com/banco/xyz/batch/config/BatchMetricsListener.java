package com.banco.xyz.batch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Component
public class BatchMetricsListener implements JobExecutionListener, SkipListener<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchMetricsListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        LOGGER.info("Inicio job={} executionId={}", jobExecution.getJobInstance().getJobName(), jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LOGGER.info("Fin job={} status={} read={} write={} skip={}", jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(), jobExecution.getStepExecutions().stream().mapToLong(step -> step.getReadCount()).sum(),
                jobExecution.getStepExecutions().stream().mapToLong(step -> step.getWriteCount()).sum(),
                jobExecution.getStepExecutions().stream().mapToLong(step -> step.getSkipCount()).sum());
    }

    @Override
    public void onSkipInRead(Throwable throwable) {
        LOGGER.warn("Registro omitido durante lectura: {}", throwable.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable throwable) {
        LOGGER.warn("Registro omitido durante proceso: {}", throwable.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable throwable) {
        LOGGER.warn("Registro omitido durante escritura: {}", throwable.getMessage());
    }
}
