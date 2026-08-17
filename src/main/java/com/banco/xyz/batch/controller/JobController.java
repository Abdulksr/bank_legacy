package com.banco.xyz.batch.controller;

import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banco.xyz.batch.exceptions.EstadoCuentaJobLauncherException;
import com.banco.xyz.batch.exceptions.InteresesJobLauncherException;
import com.banco.xyz.batch.exceptions.TransaccionesJobLauncherException;
import com.banco.xyz.batch.service.EstadoCuentaJobService;
import com.banco.xyz.batch.service.InteresesJobService;
import com.banco.xyz.batch.service.TransaccionesJobService;

@RestController
@RequestMapping("/api/batch/job")
public class JobController {

    @Autowired
    private TransaccionesJobService transaccionesJobService;

    @Autowired
    private InteresesJobService interesesJobService;

    @Autowired
    private EstadoCuentaJobService estadoCuentaJobService;

    @PostMapping("/transacciones")
    public ResponseEntity<Map<String, Object>> ejecutarTransaccionesJob() {
        JobExecution execution = transaccionesJobService.ejecutar();
        return ResponseEntity.ok(Map.of(
                "jobExecutionId", execution.getId(),
                "estado", execution.getStatus().toString(),
                "exitStatus", execution.getExitStatus().getExitCode(),
                "fechaInicio", execution.getStartTime(),
                "fechaFin", execution.getEndTime()));
    }

    @PostMapping("/intereses")
    public ResponseEntity<Map<String, Object>> ejecutarInteresesJob() {
        JobExecution execution = interesesJobService.ejecutar();
        return ResponseEntity.ok(Map.of(
                "jobExecutionId", execution.getId(),
                "estado", execution.getStatus().toString(),
                "exitStatus", execution.getExitStatus().getExitCode(),
                "fechaInicio", execution.getStartTime(),
                "fechaFin", execution.getEndTime()));
    }

    @PostMapping("/estado-cuenta")
    public ResponseEntity<Map<String, Object>> ejecutarEstadoCuentaJob() {
        JobExecution execution = estadoCuentaJobService.ejecutar();
        return ResponseEntity.ok(Map.of(
                "jobExecutionId", execution.getId(),
                "estado", execution.getStatus().toString(),
                "exitStatus", execution.getExitStatus().getExitCode(),
                "fechaInicio", execution.getStartTime(),
                "fechaFin", execution.getEndTime()));
    }

    @ExceptionHandler(InteresesJobLauncherException.class)
    public ResponseEntity<Map<String, Object>> alNoPoderIniciarElJob(InteresesJobLauncherException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TransaccionesJobLauncherException.class)
    public ResponseEntity<Map<String, Object>> alNoPoderIniciarElJob(TransaccionesJobLauncherException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EstadoCuentaJobLauncherException.class)
    public ResponseEntity<Map<String, Object>> alNoPoderIniciarElJob(EstadoCuentaJobLauncherException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

}
