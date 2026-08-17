package com.banco.xyz.batch.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

import com.banco.xyz.batch.exceptions.InteresesJobLauncherException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstadoCuentaJobService {

    private final JobLauncher jobLauncher;
    private final Job estadoCuentaJob;
    private final JobExplorer jobExplorer;

    public JobExecution ejecutar() {
        try {
            JobParametersBuilder parametersBuilder = new JobParametersBuilder(jobExplorer)
                    .getNextJobParameters(estadoCuentaJob);
            return jobLauncher.run(estadoCuentaJob, parametersBuilder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                | JobInstanceAlreadyCompleteException | JobParametersInvalidException ex) {
            log.error("No fue posible iniciar el Job estadoCuentaJob", ex);
            throw new InteresesJobLauncherException(
                    "No fue posible iniciar el Job estadoCuentaJob: " + ex.getMessage(),
                    ex);
        }
    }
}
