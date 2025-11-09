// src/main/java/edu/uade/prog3/tpo/api/ControladorPrim.java
package edu.uade.prog3.tpo.api;

import edu.uade.prog3.tpo.dominio.CriterioPeso;
import edu.uade.prog3.tpo.dominio.ResultadoPrim;
import edu.uade.prog3.tpo.servicio.ServicioPrim;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Controlador REST para ejecutar el algoritmo de Prim.
 * Permite calcular el árbol de expansión mínima (MST) completo
 * o filtrado por una zona específica (lista de IDs de hubs).
 */
@RestController
@RequestMapping("/api/grafos")
public class ControladorPrim {

    private final ServicioPrim service;

    public ControladorPrim(ServicioPrim service) {
        this.service = service;
    }

    /**
     * Ejecuta el algoritmo de Prim sobre la red logística.
     *
     * Ejemplos:
     *  http://localhost:8080/api/grafos/prim?inicioId=DEP_NORTE&criterio=DISTANCIA
     *  http://localhost:8080/api/grafos/prim?inicioId=DEP_SUR&criterio=DISTANCIA&ids=DEP_SUR,SUC_FLORES,SUC_PARQUEPATRICIOS,DEP_NORTE,SUC_PALERMO,SUC_BELGRANO,SUC_CABALLITO
     *
     * @param inicioId nodo inicial (ej. DEP_SUR)
     * @param criterio criterio de peso (DISTANCIA, TIEMPO o COSTO)
     * @param factorPeaje multiplicador si se usa COSTO
     * @param ids lista opcional de hubs separados por coma para limitar la zona
     */
    @GetMapping("/prim")
    public ResponseEntity<?> prim(
            @RequestParam String inicioId,
            @RequestParam(defaultValue = "DISTANCIA") CriterioPeso criterio,
            @RequestParam(defaultValue = "1.0") double factorPeaje,
            @RequestParam(required = false) String ids
    ) {
        try {
            // parsear la lista de IDs si viene en el query param
            List<String> filtro = null;
            if (ids != null && !ids.isBlank()) {
                filtro = Arrays.stream(ids.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }

            // ejecutar Prim con o sin filtro
            ResultadoPrim resultado = service.prim(inicioId, criterio, factorPeaje, filtro);
            return ResponseEntity.ok(resultado);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError().body("Error interno al ejecutar Prim: " + ex.getMessage());
        }
    }
}
