// src/main/java/edu/uade/prog3/tpo/api/ControladorBranchAndBound.java
package edu.uade.prog3.tpo.api;

import edu.uade.prog3.tpo.servicio.ServicioBranchAndBound;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/branch")
public class ControladorBranchAndBound {

    private final ServicioBranchAndBound servicio;

    public ControladorBranchAndBound(ServicioBranchAndBound servicio) {
        this.servicio = servicio;
    }

    /**
     * Ejemplos:
     *
     * 1) Ruta óptima (mínima distancia) pasando por Palermo y Belgrano:
     *    http://localhost:8080/branch/ruta?origen=DEP_NORTE&destino=SUC_BOEDO&paradas=SUC_PALERMO,SUC_BELGRANO
     *
     * 2) Ruta óptima hasta un cliente:
     *    http://localhost:8080/branch/ruta?origen=DEP_NORTE&destino=CLI_PAL_2&paradas=SUC_RECOLETA
     */
    @GetMapping("/ruta")
    public ResponseEntity<?> rutaOptima(
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

            Map<String, Object> resultado = servicio.rutaOptima(origen, destino, obligatorias);
            return ResponseEntity.ok(resultado);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno en ramificación y poda", "detalle", e.getMessage()));
        }
    }
}
