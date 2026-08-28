package com.banco.xyz.batch.entities;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "estado_cuenta", uniqueConstraints = @UniqueConstraint(columnNames = { "cuenta_id", "anio" }))
public class EstadoCuentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String cuentaId;
    private int cantidadTransacciones;
    private Double totalIngresos;
    private Double totalRetiros;
    private LocalDate fechaProceso;
    private int anio;
    private Double saldoFinal;

}
