package edu.uade.prog3.tpo.api;

import edu.uade.prog3.tpo.servicio.ServicioDivideYVenceras;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dyv")
public class ControladorDivideYVenceras {

    private final ServicioDivideYVenceras servicio;

    public ControladorDivideYVenceras(ServicioDivideYVenceras servicio) {
        this.servicio = servicio;
    }

    /**
     * Ejemplo:
     * http://localhost:8080/dyv/ordenarPaquetes?depositoId=DEP_NORTE
     */
    @GetMapping("/ordenarPaquetes")
    public ResponseEntity<List<Map<String, Object>>> ordenarPaquetes(
            @RequestParam String depositoId
    ) {
        List<Map<String, Object>> ordenados = servicio.ordenarPaquetesPorDyV(depositoId);
        return ResponseEntity.ok(ordenados);
    }
}
