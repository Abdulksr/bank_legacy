package com.banco.xyz.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.banco.xyz.batch.dtos.EstadoCuentaDTO;
import com.banco.xyz.batch.entities.EstadoCuentaEntity;

public class EstadoCuentaProcessor implements ItemProcessor<EstadoCuentaDTO, EstadoCuentaEntity> {

    @Override
    public EstadoCuentaEntity process(EstadoCuentaDTO item) {
        if (item == null || item.getCuentaId() == null || item.getCuentaId().isBlank() || item.getFecha() == null
                || item.getMonto() == null || item.getMonto() == 0 || item.getTransaccion() == null
                || item.getTransaccion().isBlank() ||
                !item.getTransaccion().trim().toLowerCase().equals("deposito") &&
                        !item.getTransaccion().trim().toLowerCase().equals("retiro") &&
                        !item.getTransaccion().trim().toLowerCase().equals("compra") &&
                        !item.getTransaccion().trim().toLowerCase().equals("pago")) {
            throw new InvalidBatchDataException("Movimiento anual invalido");
        }
        EstadoCuentaEntity entity = new EstadoCuentaEntity();
        entity.setCuentaId(item.getCuentaId());
        entity.setFechaProceso(item.getFecha());
        entity.setAnio(item.getFecha().getYear());
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
