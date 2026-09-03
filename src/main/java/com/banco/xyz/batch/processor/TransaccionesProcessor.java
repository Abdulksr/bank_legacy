package com.banco.xyz.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.banco.xyz.batch.dtos.TransaccionesDTO;
import com.banco.xyz.batch.entities.TransaccionesEntity;

public class TransaccionesProcessor implements ItemProcessor<TransaccionesDTO, TransaccionesEntity> {

    @Override
    public TransaccionesEntity process(TransaccionesDTO dto) {
        if (dto == null || dto.getId() == null || dto.getMonto() == null || dto.getMonto() <= 0
                || dto.getFecha() == null || dto.getTipo() == null || dto.getTipo().isBlank() ||
                !dto.getTipo().trim().toLowerCase().equals("credito") &&
                        !dto.getTipo().trim().toLowerCase().equals("debito")) {
            throw new InvalidBatchDataException("Transaccion invalida");
        }

        TransaccionesEntity entity = new TransaccionesEntity();
        entity.setId(dto.getId());
        entity.setFecha(dto.getFecha());
        entity.setMonto(dto.getMonto());
        entity.setTipo(dto.getTipo());
        return entity;
    }

}
