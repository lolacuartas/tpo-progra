// src/main/java/edu/uade/prog3/tpo/api/ControladorDFS.java
package edu.uade.prog3.tpo.api;

import edu.uade.prog3.tpo.dominio.ResultadoDFS;
import edu.uade.prog3.tpo.servicio.ServicioDFS;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grafos")
public class ControladorDFS {

    private final ServicioDFS service;

    public ControladorDFS(ServicioDFS service) { this.service = service; }

    //ejemplos de uso del endpoint:
    // http://localhost:8080/api/grafos/dfs-camino?origenId=DEP_SUR&destinoId=SUC_PALERMO&maxProfundidad=5&incluirClientes=true

    /** Busca UN camino de origen a destino con DFS. */
    @GetMapping("/dfs-camino")
    public ResponseEntity<?> dfsCamino(
            @RequestParam String origenId,
            @RequestParam String destinoId,
            @RequestParam(required = false) Integer maxProfundidad,
            @RequestParam(defaultValue = "false") boolean incluirClientes
    ) {
        try {
            ResultadoDFS r = service.dfsCamino(origenId, destinoId, maxProfundidad, incluirClientes);
            if (r.getCaminoIds().isEmpty()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /** Devuelve el orden de visita (preorden) con DFS desde origen. */
    @GetMapping("/dfs-recorrido")
    public ResponseEntity<?> dfsRecorrido(
            @RequestParam String origenId,
            @RequestParam(required = false) Integer maxProfundidad,
            @RequestParam(defaultValue = "false") boolean incluirClientes
    ) {
        try {
            ResultadoDFS r = service.dfsRecorrido(origenId, maxProfundidad, incluirClientes);
            if (r.getRecorridoIds().isEmpty()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
