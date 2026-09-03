package com.banco.xyz.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.banco.xyz.batch.dtos.InteresesDTO;
import com.banco.xyz.batch.entities.InteresesEntity;

public class InteresesProcessor implements ItemProcessor<InteresesDTO, InteresesEntity> {

    @Override
    public InteresesEntity process(InteresesDTO item) {
        if (item == null || item.getCuentaId() == null || item.getNombre() == null || item.getNombre().isBlank()
                || item.getNombre().length() <= 2 ||
                item.getSaldo() == null || item.getSaldo() <= 0 ||
                item.getEdad() == null || item.getEdad() >= 110 || item.getEdad() < 0 ||
                item.getTipo() == null || item.getTipo().isBlank() ||
                !item.getTipo().trim().toLowerCase().equals("ahorro") &&
                        !item.getTipo().trim().toLowerCase().equals("prestamo") &&
                        !item.getTipo().trim().toLowerCase().equals("hipoteca")) {
            throw new InvalidBatchDataException("Interes invalido");
        }
        Double nuevoSaldo = item.getSaldo();
        String tipoCuenta = item.getTipo().trim().toLowerCase();

        switch (tipoCuenta) {
            case "ahorro":
                nuevoSaldo += nuevoSaldo * 0.05;
                break;
            case "prestamo":
                nuevoSaldo += nuevoSaldo * 0.10;
                break;
            case "hipoteca":
                nuevoSaldo += nuevoSaldo * 0.15;
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
