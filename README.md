# Sistema de Migración Batch - Banco XYZ

Este proyecto es una solución empresarial desarrollada con **Spring Boot** y **Spring Batch** para modernizar y procesar la información heredada (Legacy) del Banco XYZ. Se enfoca en el procesamiento masivo de datos mediante la lectura de archivos CSV y su persistencia optimizada en una base de datos MySQL.

## Arquitectura y Procesos Clave

El sistema cuenta con tres Jobs principales (procesos Batch) expuestos mediante una API REST para su ejecución bajo demanda:

### 1. Job de Transacciones Diarias (`/api/batch/job/transacciones`)
Lee el archivo `transacciones.csv`, valida la consistencia de los datos (filtrando transacciones con montos nulos o negativos) y registra la información validada en la tabla `transacciones`.

### 2. Job de Cálculo de Intereses Mensuales (`/api/batch/job/intereses`)
Procesa el archivo `intereses.csv`. Aplica la lógica de negocio bancaria correspondiente:
- **Cuentas de Ahorro:** Bonificación del 5% al saldo.
- **Préstamos:** Incremento del 10% a la deuda.
Guarda el resultado como un registro de log en la tabla `intereses` utilizando llaves primarias autoincrementales para mantener un historial limpio.

### 3. Job de Estados de Cuenta Anuales (`/api/batch/job/estados-cuenta`)
Genera un resumen financiero anual a partir de `cuentas_anuales.csv`. Utiliza una técnica avanzada de **Upsert (Insert on Duplicate Key Update)** en MySQL para acumular sobre la marcha:
- Saldo final calculado.
- Cantidad total de transacciones en el año.
- Sumatoria de ingresos y egresos.
- Fecha del último movimiento registrado.

## Tecnologías Utilizadas

- **Java 17+**
- **Spring Boot 3.2.x**
- **Spring Batch 5.x** (Arquitectura de Chunk-oriented processing)
- **Spring Data JPA** & **JDBC**
- **MySQL 8+** (Driver Connector/J)
- **Lombok**

## Configuración y Ejecución

1. **Base de datos:**
   Es necesario asegurarse de tener un servidor MySQL en ejecución en el puerto `3306` con una base de datos llamada `banco_xyz_batch` y las credenciales correspondientes configuradas en el archivo `src/main/resources/application.properties`.

2. **Arranque:**
   Se debe ejecutar la clase principal `BatchApplication.java` desde el entorno de desarrollo o empaquetar la aplicación con Maven utilizando los siguientes comandos:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

3. **Ejecución de Procesos (Postman):**
   Para iniciar los procesos, es necesario enviar peticiones HTTP `POST` a los siguientes endpoints:
   - `http://localhost:8080/api/batch/job/transacciones`
   - `http://localhost:8080/api/batch/job/intereses`
    - `http://localhost:8080/api/batch/job/estado-cuenta`

   Ejemplos con `curl`:
   ```bash
   curl -X POST http://localhost:8080/api/batch/job/transacciones
   curl -X POST http://localhost:8080/api/batch/job/intereses
   curl -X POST http://localhost:8080/api/batch/job/estado-cuenta
   ```

> **Configuración S2:** cada step procesa chunks de 5 con un executor fijo de 3 hilos, reader sincronizado, skip de datos inválidos (máximo 10) y hasta 3 reintentos de errores transitorios.

---
*Proyecto desarrollado como actividad práctica de modernización de sistemas legacy con Spring Batch.*
