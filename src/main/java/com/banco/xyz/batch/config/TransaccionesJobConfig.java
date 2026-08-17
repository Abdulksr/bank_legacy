package com.banco.xyz.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

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

@Configuration
public class TransaccionesJobConfig {

    private static final int CHUNK_SIZE = 10;

    @Value("${ruta.archivo.transacciones:classpath:data/semana_1/transacciones.csv}")
    private Resource archivoTransacciones;

    @Bean
    public FlatFileItemReader<TransaccionesDTO> transaccionesItemReader() {
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

        return new FlatFileItemReaderBuilder<TransaccionesDTO>()
                .name("transaccionesItemReader")
                .resource(archivoTransacciones)
                .linesToSkip(1)
                .lineMapper(lineMapper)
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
            FlatFileItemReader<TransaccionesDTO> transaccionesItemReader,
            TransaccionesProcessor transaccionesItemProcessor,
            JdbcBatchItemWriter<TransaccionesEntity> transaccionesItemWriter) {
        return new StepBuilder("transaccionesStep", jobRepository)
                .<TransaccionesDTO, TransaccionesEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(transaccionesItemReader)
                .processor(transaccionesItemProcessor)
                .writer(transaccionesItemWriter)
                .build();
    }

    @Bean
    public Job transaccionesJob(JobRepository jobRepository, Step transaccionesStep) {
        return new JobBuilder("transaccionesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(transaccionesStep)
                .build();
    }

}
