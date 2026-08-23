package com.banco.xyz.batch.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.banco.xyz.batch.dtos.EstadoCuentaDTO;
import com.banco.xyz.batch.entities.EstadoCuentaEntity;
import com.banco.xyz.batch.resilience.DataQualityException;

@Component
public class EstadoCuentaProcessor implements ItemProcessor<EstadoCuentaDTO, EstadoCuentaEntity> {

    @Override
    public EstadoCuentaEntity process(EstadoCuentaDTO item) {
        if (item.getCuentaId() == null || item.getFecha() == null || item.getMonto() == null) {
            throw new DataQualityException("Movimiento de estado de cuenta inválido");
        }
        EstadoCuentaEntity entity = new EstadoCuentaEntity();
        entity.setCuentaId(item.getCuentaId());
        entity.setFechaProceso(item.getFecha());
        entity.setCantidadTransacciones(1);
        if (item.getMonto() > 0) {
            entity.setTotalIngresos(item.getMonto());
            entity.setTotalRetiros(0.0);
        } else {
            entity.setTotalIngresos(0.0);
            entity.setTotalRetiros(item.getMonto());
        }
        entity.setSaldoFinal(item.getMonto());

        return entity;
    }

}
