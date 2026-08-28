package com.banco.xyz.batch.config;

import java.time.LocalDate;

import javax.sql.DataSource;

import com.banco.xyz.batch.dtos.EstadoCuentaDTO;
import com.banco.xyz.batch.entities.EstadoCuentaEntity;
import com.banco.xyz.batch.partitioner.CsvPartitioner;
import com.banco.xyz.batch.processor.EstadoCuentaProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class EstadoCuentaJobConfig {

    private static final int CHUNK_SIZE = 5;

    @Bean
    @StepScope
    public FlatFileItemReader<EstadoCuentaDTO> estadoCuentaItemReader(
            @Value("${ruta.archivo.estadoCuenta:classpath:data/semana_3/cuentas_anuales.csv}") Resource archivo,
            @Value("#{stepExecutionContext['startLine']}") int startLine,
            @Value("#{stepExecutionContext['maxItems']}") int maxItems) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuentaId", "fecha", "transaccion", "monto", "descripcion");
        BeanWrapperFieldSetMapper<EstadoCuentaDTO> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(EstadoCuentaDTO.class);
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverter(String.class, LocalDate.class, LocalDate::parse);
        mapper.setConversionService(conversionService);
        DefaultLineMapper<EstadoCuentaDTO> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);
        return new FlatFileItemReaderBuilder<EstadoCuentaDTO>()
                .name("estadoCuentaItemReader")
                .resource(archivo)
                .linesToSkip(startLine)
                .maxItemCount(maxItems)
                .lineMapper(lineMapper)
                .build();
    }

    @Bean
    public EstadoCuentaProcessor estadoCuentaItemProcessor() {
        return new EstadoCuentaProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<EstadoCuentaEntity> estadoCuentaItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<EstadoCuentaEntity>().dataSource(dataSource)
                .sql("INSERT INTO estado_cuenta (cuenta_id, cantidad_transacciones, total_ingresos, total_retiros, fecha_proceso, anio, saldo_final) VALUES (:cuentaId, :cantidadTransacciones, :totalIngresos, :totalRetiros, :fechaProceso, :anio, :saldoFinal) ON DUPLICATE KEY UPDATE saldo_final = saldo_final + VALUES(saldo_final), cantidad_transacciones = cantidad_transacciones + VALUES(cantidad_transacciones), total_ingresos = total_ingresos + VALUES(total_ingresos), total_retiros = total_retiros + VALUES(total_retiros), fecha_proceso = GREATEST(fecha_proceso, VALUES(fecha_proceso)), anio = GREATEST(anio, VALUES(anio))")
                .beanMapped().build();
    }

    @Bean
    public Step estadoCuentaStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            FlatFileItemReader<EstadoCuentaDTO> estadoCuentaItemReader,
            EstadoCuentaProcessor estadoCuentaItemProcessor,
            JdbcBatchItemWriter<EstadoCuentaEntity> estadoCuentaItemWriter,
            InvalidDataSkipPolicy invalidDataSkipPolicy,
            BatchMetricsListener batchMetricsListener) {
        return new StepBuilder("estadoCuentaStep", jobRepository)
                .<EstadoCuentaDTO, EstadoCuentaEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(estadoCuentaItemReader).processor(estadoCuentaItemProcessor).writer(estadoCuentaItemWriter)
                .faultTolerant().skipPolicy(invalidDataSkipPolicy).retry(TransientDataAccessException.class)
                .retryLimit(3)
                .backOffPolicy(new ExponentialBackOffPolicy())
                .listener(batchMetricsListener).build();
    }

    @Bean
    public TaskExecutorPartitionHandler estadoCuentaPartitionHandler(Step estadoCuentaStep,
            @Qualifier("partitionTaskExecutor") TaskExecutor partitionTaskExecutor) {

        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(estadoCuentaStep);
        partitionHandler.setTaskExecutor(partitionTaskExecutor);
        partitionHandler.setGridSize(5);
        return partitionHandler;
    }

    @Bean
    public Step estadoCuentaMasterStep(JobRepository jobRepository,
            PartitionHandler estadoCuentaPartitionHandler,
            @Value("${ruta.archivo.estadoCuenta:classpath:data/semana_3/cuentas_anuales.csv}") Resource archivo) {

        CsvPartitioner partitioner = new CsvPartitioner(archivo);

        return new StepBuilder("estadoCuentaMasterStep", jobRepository)
                .partitioner("estadoCuentaStep", partitioner)
                .partitionHandler(estadoCuentaPartitionHandler)
                .build();
    }

    @Bean
    public Job estadoCuentaJob(JobRepository jobRepository, Step estadoCuentaMasterStep,
            BatchMetricsListener batchMetricsListener) {
        return new JobBuilder("estadoCuentaJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(batchMetricsListener)
                .start(estadoCuentaMasterStep)
                .build();
    }
}
