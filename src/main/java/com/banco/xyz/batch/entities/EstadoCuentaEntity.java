package com.banco.xyz.batch.entities;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "estado_cuenta")
public class EstadoCuentaEntity {

    @Id
    private String cuentaId;
    private int cantidadTransacciones;
    private Double totalIngresos;
    private Double totalRetiros;
    private LocalDate fechaProceso;
    private Double saldoFinal;

}
