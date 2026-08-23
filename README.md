# Sistema de Migración Batch - Banco XYZ

Implementación Semana 2 con Spring Boot, Spring Batch y MySQL. Los tres procesos convierten CSV en entidades procesadas y las persisten por JDBC en MySQL; se mantienen los lanzadores REST existentes.

## Jobs S2

| Job | CSV de entrada | Destino MySQL | Endpoint POST |
| --- | --- | --- | --- |
| `transaccionesJob` | `transacciones.csv` | `transacciones` | `/api/batch/job/transacciones` |
| `interesesJob` | `intereses.csv` | `intereses` | `/api/batch/job/intereses` |
| `estadoCuentaJob` | `cuentas_anuales.csv` | `estado_cuenta` | `/api/batch/job/estado-cuenta` |

Cada step usa exactamente chunks de 5 registros. Su `FlatFileItemReader` está encapsulado en un `SynchronizedItemStreamReader` y el step se ejecuta con un `ThreadPoolTaskExecutor` fijo de 3 hilos (`batch-worker-*`).

## Resiliencia y operación

- `DataQualitySkipPolicy` omite exclusivamente errores de calidad o parsing y tiene un límite explícito de 10 omisiones por step.
- Los errores transitorios de acceso a datos se reintentan como máximo 3 veces.
- `OperationalBatchListener` registra inicio, término y las omisiones; al finalizar entrega métricas de lectura, escritura y skips.
- Los jobs usan `JobRepository` y `RunIdIncrementer`. Cada POST obtiene el siguiente `run.id` mediante `JobExplorer`, por lo que una nueva ejecución crea una instancia distinta y permite reejecutar el mismo job.

## Requisitos

- Java 17.
- Maven 3.9 o compatible.
- MySQL 8 en `localhost:3306`, base `banco_xyz_batch`, o credenciales equivalentes configuradas en `src/main/resources/application.properties`.

## Ejecución

```bash
mvn test
mvn spring-boot:run
```

Los CSV por defecto están en `src/main/resources/data/semana_1`. Pueden cambiarse al iniciar la aplicación:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--ruta.archivo.transacciones=file:/ruta/transacciones.csv --ruta.archivo.intereses=file:/ruta/intereses.csv --ruta.archivo.estadoCuenta=file:/ruta/cuentas_anuales.csv"
```

Ejecute un job con `POST`:

```bash
curl -X POST http://localhost:8080/api/batch/job/transacciones
curl -X POST http://localhost:8080/api/batch/job/intereses
curl -X POST http://localhost:8080/api/batch/job/estado-cuenta
```

La respuesta incluye `jobExecutionId`, estado y fechas de la ejecución. Repetir el mismo POST genera el siguiente `run.id`; no se requiere modificar el CSV para reejecutar.

## Evidencia operativa esperada

En los logs debe observarse el inicio y término de cada job con su `executionId`, estado final, contadores `read`, `write` y `skip`, además de un `WARN` por cada registro omitido. Si se supera el límite de 10 problemas de calidad o parsing en un step, la ejecución debe fallar. Los errores transitorios de datos deben mostrar hasta tres intentos antes de propagarse.
