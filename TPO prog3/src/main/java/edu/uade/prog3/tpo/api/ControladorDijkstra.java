// src/main/java/edu/uade/prog3/tpo/api/ControladorDijkstra.java
package edu.uade.prog3.tpo.api;

import edu.uade.prog3.tpo.dominio.CriterioPeso;
import edu.uade.prog3.tpo.dominio.ResultadoDijkstra;
import edu.uade.prog3.tpo.servicio.ServicioDijkstra;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grafos")
public class ControladorDijkstra {

    private final ServicioDijkstra service;

    public ControladorDijkstra(ServicioDijkstra service) {
        this.service = service;
    }

    /**
     * Camino mínimo por Dijkstra entre Hubs usando :RUTA (con pesos).
     * Ejemplos:
     *  http://localhost:8080/api/grafos/dijkstra?origenId=DEP_SUR&destinoId=CLI_NOR_MULTI_2&criterio=DISTANCIA
     *  http://localhost:8080/api/grafos/dijkstra?origenId=DEP_SUR&destinoId=SUC_PALERMO&criterio=TIEMPO
     *  /api/grafos/dijkstra?origenId=DEP_SUR&destinoId=SUC_PALERMO&criterio=COSTO&factorPeaje=1.0
     */

    @GetMapping("/dijkstra")
    public ResponseEntity<?> dijkstra(
            @RequestParam String origenId,
            @RequestParam String destinoId,
            @RequestParam(defaultValue = "DISTANCIA") CriterioPeso criterio,
            @RequestParam(defaultValue = "1.0") double factorPeaje
    ) {
        try {
            ResultadoDijkstra r = service.dijkstra(origenId, destinoId, criterio, factorPeaje);
            if (r.getCaminoIds().isEmpty()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
