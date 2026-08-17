package com.banco.xyz.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.banco.xyz.batch.dtos.InteresesDTO;
import com.banco.xyz.batch.entities.InteresesEntity;

public class InteresesProcessor implements ItemProcessor<InteresesDTO, InteresesEntity> {

    @Override
    public InteresesEntity process(InteresesDTO item) throws Exception {

        Double nuevoSaldo = item.getSaldo();
        String tipoCuenta = item.getTipo().trim().toLowerCase();

        switch (tipoCuenta) {
            case "ahorro":
                nuevoSaldo += nuevoSaldo * 0.05;
                break;
            case "prestamo":
                nuevoSaldo += nuevoSaldo * 0.10;
                break;
            default:
                break;
        }

        InteresesEntity entity = new InteresesEntity();
        entity.setCuentaId(item.getCuentaId());
        entity.setNombre(item.getNombre());
        entity.setSaldo(nuevoSaldo);
        entity.setEdad(item.getEdad());
        entity.setTipo(item.getTipo());

        return entity;
    }

}
