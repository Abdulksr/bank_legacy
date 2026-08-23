package com.banco.xyz.batch.config;

import java.time.LocalDate;

import javax.sql.DataSource;

import com.banco.xyz.batch.dtos.TransaccionesDTO;
import com.banco.xyz.batch.entities.TransaccionesEntity;
import com.banco.xyz.batch.processor.TransaccionesProcessor;
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
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransaccionesJobConfig {

    private static final int CHUNK_SIZE = 5;

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<TransaccionesDTO> transaccionesItemReader(
            @Value("${ruta.archivo.transacciones:classpath:data/semana_2/transacciones.csv}") Resource archivo) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "fecha", "monto", "tipo");
        BeanWrapperFieldSetMapper<TransaccionesDTO> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(TransaccionesDTO.class);
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverter(String.class, LocalDate.class, LocalDate::parse);
        mapper.setConversionService(conversionService);
        DefaultLineMapper<TransaccionesDTO> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);
        FlatFileItemReader<TransaccionesDTO> delegate = new FlatFileItemReaderBuilder<TransaccionesDTO>()
                .name("transaccionesItemReader").resource(archivo).linesToSkip(1).lineMapper(lineMapper).build();
        return new SynchronizedItemStreamReaderBuilder<TransaccionesDTO>().delegate(delegate).build();
    }

    @Bean
    public TransaccionesProcessor transaccionesItemProcessor() {
        return new TransaccionesProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<TransaccionesEntity> transaccionesItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<TransaccionesEntity>().dataSource(dataSource)
                .sql("INSERT INTO transacciones (id, fecha, monto, tipo) VALUES (:id, :fecha, :monto, :tipo) ON DUPLICATE KEY UPDATE fecha = VALUES(fecha), monto = VALUES(monto), tipo = VALUES(tipo)")
                .beanMapped().build();
    }

    @Bean
    public Step transaccionesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<TransaccionesDTO> transaccionesItemReader,
            TransaccionesProcessor transaccionesItemProcessor, JdbcBatchItemWriter<TransaccionesEntity> transaccionesItemWriter,
            @Qualifier("batchTaskExecutor") TaskExecutor batchTaskExecutor, InvalidDataSkipPolicy invalidDataSkipPolicy,
            BatchMetricsListener batchMetricsListener) {
        return new StepBuilder("transaccionesStep", jobRepository)
                .<TransaccionesDTO, TransaccionesEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(transaccionesItemReader).processor(transaccionesItemProcessor).writer(transaccionesItemWriter)
                .faultTolerant().skipPolicy(invalidDataSkipPolicy).retry(TransientDataAccessException.class).retryLimit(3)
                .listener(batchMetricsListener).taskExecutor(batchTaskExecutor).throttleLimit(3).build();
    }

    @Bean
    public Job transaccionesJob(JobRepository jobRepository, Step transaccionesStep, BatchMetricsListener batchMetricsListener) {
        return new JobBuilder("transaccionesJob", jobRepository).incrementer(new RunIdIncrementer())
                .listener(batchMetricsListener).start(transaccionesStep).build();
    }
}
