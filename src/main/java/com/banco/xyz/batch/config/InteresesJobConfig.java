package com.banco.xyz.batch.config;

import javax.sql.DataSource;

import com.banco.xyz.batch.dtos.InteresesDTO;
import com.banco.xyz.batch.entities.InteresesEntity;
import com.banco.xyz.batch.processor.InteresesProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
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
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class InteresesJobConfig {

    private static final int CHUNK_SIZE = 5;

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<InteresesDTO> interesesItemReader(
            @Value("${ruta.archivo.intereses:classpath:data/semana_2/intereses.csv}") Resource archivo) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuentaId", "nombre", "saldo", "edad", "tipo");
        BeanWrapperFieldSetMapper<InteresesDTO> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(InteresesDTO.class);
        DefaultLineMapper<InteresesDTO> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);
        FlatFileItemReader<InteresesDTO> delegate = new FlatFileItemReaderBuilder<InteresesDTO>()
                .name("interesesItemReader").resource(archivo).linesToSkip(1).lineMapper(lineMapper).build();
        return new SynchronizedItemStreamReaderBuilder<InteresesDTO>().delegate(delegate).build();
    }

    @Bean
    public InteresesProcessor interesesItemProcessor() {
        return new InteresesProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<InteresesEntity> interesesItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<InteresesEntity>().dataSource(dataSource)
                .sql("INSERT INTO intereses (cuenta_id, nombre, saldo, edad, tipo) VALUES (:cuentaId, :nombre, :saldo, :edad, :tipo) ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), saldo = VALUES(saldo), edad = VALUES(edad), tipo = VALUES(tipo)")
                .beanMapped().build();
    }

    @Bean
    public Step interesesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<InteresesDTO> interesesItemReader, InteresesProcessor interesesItemProcessor,
            JdbcBatchItemWriter<InteresesEntity> interesesItemWriter,
            @Qualifier("batchTaskExecutor") TaskExecutor batchTaskExecutor,
            InvalidDataSkipPolicy invalidDataSkipPolicy, BatchMetricsListener batchMetricsListener) {
        return new StepBuilder("interesesStep", jobRepository).<InteresesDTO, InteresesEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(interesesItemReader).processor(interesesItemProcessor).writer(interesesItemWriter)
                .faultTolerant().skipPolicy(invalidDataSkipPolicy).retry(TransientDataAccessException.class).retryLimit(3)
                .listener(batchMetricsListener).taskExecutor(batchTaskExecutor).throttleLimit(3).build();
    }

    @Bean
    public Job interesesJob(JobRepository jobRepository, Step interesesStep, BatchMetricsListener batchMetricsListener) {
        return new JobBuilder("interesesJob", jobRepository).incrementer(new RunIdIncrementer())
                .listener(batchMetricsListener).start(interesesStep).build();
    }
}
