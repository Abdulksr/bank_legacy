package com.banco.xyz.batch.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "intereses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteresesEntity {
    
    @Id
    private Long cuentaId;
    private String nombre;
    private Double saldo;
    private Integer edad;
    private String tipo;
}
