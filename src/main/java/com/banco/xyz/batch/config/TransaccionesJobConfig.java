package com.banco.xyz.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.core.task.TaskExecutor;

import com.banco.xyz.batch.dtos.TransaccionesDTO;
import com.banco.xyz.batch.entities.TransaccionesEntity;
import com.banco.xyz.batch.processor.TransaccionesProcessor;

import java.time.LocalDate;

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
public class TransaccionesJobConfig {

    private static final int CHUNK_SIZE = 5;

    @Value("${ruta.archivo.transacciones:classpath:data/semana_1/transacciones.csv}")
    private Resource archivoTransacciones;

    @Bean
    public SynchronizedItemStreamReader<TransaccionesDTO> transaccionesItemReader() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "fecha", "monto", "tipo");

        BeanWrapperFieldSetMapper<TransaccionesDTO> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(TransaccionesDTO.class);
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverter(String.class, LocalDate.class, LocalDate::parse);
        fieldSetMapper.setConversionService(conversionService);

        DefaultLineMapper<TransaccionesDTO> lineMapper = new DefaultLineMapper<>();
        lineMapper.setFieldSetMapper(fieldSetMapper);
        lineMapper.setLineTokenizer(tokenizer);

        FlatFileItemReader<TransaccionesDTO> delegate = new FlatFileItemReaderBuilder<TransaccionesDTO>()
                .name("transaccionesItemReader")
                .resource(archivoTransacciones)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
        return new SynchronizedItemStreamReaderBuilder<TransaccionesDTO>()
                .delegate(delegate)
                .build();
    }

    @Bean
    public TransaccionesProcessor transaccionesItemProcessor() {
        return new TransaccionesProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<TransaccionesEntity> transaccionesItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<TransaccionesEntity>()
                .dataSource(dataSource)
                .sql("INSERT INTO transacciones (id, fecha, monto, tipo) VALUES (:id, :fecha, :monto, :tipo)")
                .beanMapped()
                .build();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean
    public Step transaccionesStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<TransaccionesDTO> transaccionesItemReader,
            TransaccionesProcessor transaccionesItemProcessor,
            JdbcBatchItemWriter<TransaccionesEntity> transaccionesItemWriter,
            @Qualifier("batchTaskExecutor") TaskExecutor batchTaskExecutor,
            OperationalBatchListener operationalBatchListener) {
        return new StepBuilder("transaccionesStep", jobRepository)
                .<TransaccionesDTO, TransaccionesEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(transaccionesItemReader)
                .processor(transaccionesItemProcessor)
                .writer(transaccionesItemWriter)
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
    public Job transaccionesJob(JobRepository jobRepository, Step transaccionesStep,
            OperationalBatchListener operationalBatchListener) {
        return new JobBuilder("transaccionesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(operationalBatchListener)
                .start(transaccionesStep)
                .build();
    }

}
