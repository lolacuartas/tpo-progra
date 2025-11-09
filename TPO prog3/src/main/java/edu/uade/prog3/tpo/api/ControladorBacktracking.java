// src/main/java/edu/uade/prog3/tpo/api/ControladorBacktracking.java
package edu.uade.prog3.tpo.api;

import edu.uade.prog3.tpo.servicio.ServicioBacktracking;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/backtracking")
public class ControladorBacktracking {

    private final ServicioBacktracking servicio;

    public ControladorBacktracking(ServicioBacktracking servicio) {
        this.servicio = servicio;
    }

    /**
     * Ejemplos:
     *
     * 1) Origen → destino (sin paradas):
     *    http://localhost:8080/backtracking/ruta?origen=DEP_NORTE&destino=SUC_BOEDO
     *
     * 2) Origen → destino HUB pasando por 2 sucursales:
     *    http://localhost:8080/backtracking/ruta?origen=DEP_NORTE&destino=SUC_BOEDO&paradas=SUC_PALERMO,SUC_BELGRANO
     *
     * 3) Origen → CLIENTE pasando por una sucursal:
     *    http://localhost:8080/backtracking/ruta?origen=DEP_NORTE&destino=CLI_PAL_2&paradas=SUC_RECOLETA
     */
    @GetMapping("/ruta")
    public ResponseEntity<?> rutaConParadas(
            @RequestParam String origen,
            @RequestParam String destino,
            @RequestParam(required = false) String paradas
    ) {
        try {
            List<String> obligatorias = new ArrayList<>();
            if (paradas != null && !paradas.isBlank()) {
                for (String p : paradas.split(",")) {
                    obligatorias.add(p.trim());
                }
            }

            Map<String, Object> res = servicio.rutaConParadas(origen, destino, obligatorias);
            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno en backtracking", "detalle", e.getMessage()));
        }
    }
}
