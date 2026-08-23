package com.banco.xyz.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.core.task.TaskExecutor;

import com.banco.xyz.batch.dtos.EstadoCuentaDTO;
import org.springframework.core.convert.support.DefaultConversionService;
import java.time.LocalDate;

import com.banco.xyz.batch.entities.EstadoCuentaEntity;

import com.banco.xyz.batch.processor.EstadoCuentaProcessor;

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
public class EstadoCuentaJobConfig {

    @Value("${ruta.archivo.estadoCuenta:classpath:data/semana_1/cuentas_anuales.csv}")
    private Resource archivoCuentasAnuales;

    private static final int CHUNK_SIZE = 5;

    @Bean
    public SynchronizedItemStreamReader<EstadoCuentaDTO> estadoCuentaItemReader() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuentaId", "fecha", "transaccion", "monto", "descripcion");

        BeanWrapperFieldSetMapper<EstadoCuentaDTO> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(EstadoCuentaDTO.class);
        
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverter(String.class, LocalDate.class, LocalDate::parse);
        fieldSetMapper.setConversionService(conversionService);

        DefaultLineMapper<EstadoCuentaDTO> lineMapper = new DefaultLineMapper<>();
        lineMapper.setFieldSetMapper(fieldSetMapper);
        lineMapper.setLineTokenizer(tokenizer);

        FlatFileItemReader<EstadoCuentaDTO> delegate = new FlatFileItemReaderBuilder<EstadoCuentaDTO>()
                .name("estadoCuentaItemReader")
                .resource(archivoCuentasAnuales)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
        return new SynchronizedItemStreamReaderBuilder<EstadoCuentaDTO>()
                .delegate(delegate)
                .build();
    }

    @Bean
    public EstadoCuentaProcessor estadoCuentaItemProcessor() {
        return new EstadoCuentaProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<EstadoCuentaEntity> estadoCuentaItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<EstadoCuentaEntity>()
                .dataSource(dataSource)
                .sql("INSERT INTO estado_cuenta(cuenta_id,cantidad_transacciones,total_ingresos,total_retiros,fecha_proceso,saldo_final)VALUES(:cuentaId,:cantidadTransacciones,:totalIngresos,:totalRetiros,:fechaProceso,:saldoFinal) ON DUPLICATE KEY UPDATE saldo_final = saldo_final + VALUES(saldo_final), cantidad_transacciones = cantidad_transacciones + 1, total_ingresos = total_ingresos + VALUES(total_ingresos), total_retiros = total_retiros + VALUES(total_retiros), fecha_proceso = GREATEST(fecha_proceso, VALUES(fecha_proceso))")
                .beanMapped()
                .build();
    }

    @Bean
    public Step estadoCuentaStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<EstadoCuentaDTO> estadoCuentaItemReader,
            EstadoCuentaProcessor estadoCuentaItemProcessor,
            JdbcBatchItemWriter<EstadoCuentaEntity> estadoCuentaItemWriter,
            @Qualifier("batchTaskExecutor") TaskExecutor batchTaskExecutor,
            OperationalBatchListener operationalBatchListener) {
        return new StepBuilder("estadoCuentaStep", jobRepository)
                .<EstadoCuentaDTO, EstadoCuentaEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(estadoCuentaItemReader)
                .processor(estadoCuentaItemProcessor)
                .writer(estadoCuentaItemWriter)
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
    public Job estadoCuentaJob(JobRepository jobRepository, Step estadoCuentaStep,
            OperationalBatchListener operationalBatchListener) {
        return new JobBuilder("estadoCuentaJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(operationalBatchListener)
                .start(estadoCuentaStep)
                .build();
    }

}
