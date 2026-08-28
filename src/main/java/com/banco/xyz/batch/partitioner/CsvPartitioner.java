package com.banco.xyz.batch.partitioner;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;

public class CsvPartitioner implements Partitioner {

    private final Resource archivo;

    public CsvPartitioner(Resource archivo) {
        this.archivo = archivo;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();

        try {
            int totalLines = (int) Files.lines(Paths.get(archivo.getURI())).count() - 1;

            int lineasPorPartition = totalLines / gridSize;

            for (int i = 0; i < gridSize; i++) {
                ExecutionContext context = new ExecutionContext();
                int inicio = (i * lineasPorPartition) + 1;
                int cantidadLeer = lineasPorPartition;

                if (i == gridSize - 1) {
                    cantidadLeer = totalLines - (inicio - 1);
                }

                context.put("startLine", inicio);
                context.put("maxItems", cantidadLeer);
                partitions.put("partition-" + archivo.getFilename() + "-" + i, context);
            }

        } catch (Exception e) {
            System.out.println("Error al generar las particiones: " + e.getMessage());
            throw new RuntimeException("Error al generar las particiones", e);
        }

        return partitions;
    }

}
