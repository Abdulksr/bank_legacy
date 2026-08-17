package com.banco.xyz.batch.entities;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transacciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionesEntity {
    @Id
    private Long id;
    private LocalDate fecha;
    private Double monto;
    private String tipo;
}
