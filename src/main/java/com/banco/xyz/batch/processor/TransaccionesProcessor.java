package com.banco.xyz.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.banco.xyz.batch.dtos.TransaccionesDTO;
import com.banco.xyz.batch.entities.TransaccionesEntity;

public class TransaccionesProcessor implements ItemProcessor<TransaccionesDTO, TransaccionesEntity> {

    @Override
    public TransaccionesEntity process(TransaccionesDTO dto) throws Exception {

        if (dto.getMonto() == 0 || dto.getMonto() < 0 || dto.getFecha() == null) {
            System.out.println("Anomalia detectada y descartada en la transaccion: " + dto);
            return null;
        }

        TransaccionesEntity entity = new TransaccionesEntity();
        entity.setId(dto.getId());
        entity.setFecha(dto.getFecha());
        entity.setMonto(dto.getMonto());
        entity.setTipo(dto.getTipo());
        return entity;
    }

}
