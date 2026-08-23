package com.banco.xyz.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.core.task.TaskExecutor;

import com.banco.xyz.batch.dtos.InteresesDTO;

import com.banco.xyz.batch.entities.InteresesEntity;

import com.banco.xyz.batch.processor.InteresesProcessor;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.dao.TransientDataAccessException;

import com.banco.xyz.batch.resilience.DataQualitySkipPolicy;
import com.banco.xyz.batch.resilience.OperationalBatchListener;

@Configuration
public class InteresesJobConfig {

    @Value("${ruta.archivo.intereses:classpath:data/semana_1/intereses.csv}")
    private Resource archivoIntereses;

    private static final int CHUNK_SIZE = 5;

    @Bean
    public SynchronizedItemStreamReader<InteresesDTO> interesesItemReader() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuenta_id", "nombre", "saldo", "edad", "tipo");

        BeanWrapperFieldSetMapper<InteresesDTO> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(InteresesDTO.class);

        DefaultLineMapper<InteresesDTO> lineMapper = new DefaultLineMapper<>();
        lineMapper.setFieldSetMapper(fieldSetMapper);
        lineMapper.setLineTokenizer(tokenizer);

        FlatFileItemReader<InteresesDTO> delegate = new FlatFileItemReaderBuilder<InteresesDTO>()
                .name("interesesItemReader")
                .resource(archivoIntereses)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
        return new SynchronizedItemStreamReaderBuilder<InteresesDTO>()
                .delegate(delegate)
                .build();
    }

    @Bean
    public InteresesProcessor interesesItemProcessor() {
        return new InteresesProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<InteresesEntity> interesesItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<InteresesEntity>()
                .dataSource(dataSource)
                .sql("INSERT INTO intereses (cuenta_id, nombre, saldo, edad, tipo) VALUES (:cuentaId, :nombre, :saldo, :edad, :tipo) ON DUPLICATE KEY UPDATE saldo = VALUES(saldo)")
                .beanMapped()
                .build();
    }

    @Bean
    public Step interesesStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<InteresesDTO> interesesItemReader,
            InteresesProcessor interesesItemProcessor,
            JdbcBatchItemWriter<InteresesEntity> interesesItemWriter,
            @Qualifier("batchTaskExecutor") TaskExecutor batchTaskExecutor,
            OperationalBatchListener operationalBatchListener) {
        return new StepBuilder("interesesStep", jobRepository)
                .<InteresesDTO, InteresesEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(interesesItemReader)
                .processor(interesesItemProcessor)
                .writer(interesesItemWriter)
                .faultTolerant()
                .skipPolicy(new DataQualitySkipPolicy())
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .listener(operationalBatchListener)
                .taskExecutor(batchTaskExecutor)
                .throttleLimit(3)
                .build();
    }

    @Bean
    public Job interesesJob(JobRepository jobRepository, Step interesesStep,
            OperationalBatchListener operationalBatchListener) {
        return new JobBuilder("interesesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(operationalBatchListener)
                .start(interesesStep)
                .build();
    }

}
