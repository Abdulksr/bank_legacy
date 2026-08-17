package com.banco.xyz.batch.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteresesDTO {
    private Long cuentaId;
    private String nombre;
    private Double saldo;
    private Integer edad;
    private String tipo;
}
