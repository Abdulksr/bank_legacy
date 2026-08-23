package com.banco.xyz.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.banco.xyz.batch.dtos.TransaccionesDTO;
import com.banco.xyz.batch.entities.TransaccionesEntity;
import com.banco.xyz.batch.resilience.DataQualityException;

public class TransaccionesProcessor implements ItemProcessor<TransaccionesDTO, TransaccionesEntity> {

    @Override
    public TransaccionesEntity process(TransaccionesDTO dto) {
        if (dto.getMonto() == 0 || dto.getMonto() < 0 || dto.getFecha() == null) {
            throw new DataQualityException("Transacción con monto o fecha inválidos");
        }

        TransaccionesEntity entity = new TransaccionesEntity();
        entity.setId(dto.getId());
        entity.setFecha(dto.getFecha());
        entity.setMonto(dto.getMonto());
        entity.setTipo(dto.getTipo());
        return entity;
    }

}
