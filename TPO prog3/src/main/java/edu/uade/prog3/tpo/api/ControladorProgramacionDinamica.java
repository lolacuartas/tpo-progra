package edu.uade.prog3.tpo.api;

import edu.uade.prog3.tpo.servicio.ServicioProgramacionDinamica;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dp")
public class ControladorProgramacionDinamica {

    private final ServicioProgramacionDinamica servicio;

    public ControladorProgramacionDinamica(ServicioProgramacionDinamica servicio) {
        this.servicio = servicio;
    }

    /**
     * Ejemplo:
     * http://localhost:8080/dp/mochila?depositoId=DEP_NORTE&vehiculoId=CAMIONETA_DEP_NORTE&destino=CLI_NOR_MULTI_1
     */
    @GetMapping("/mochila")
    public ResponseEntity<Map<String, Object>> ejecutar(
            @RequestParam String depositoId,
            @RequestParam(required = false) String vehiculoId,
            @RequestParam(required = false) String destino
    ) {
        Map<String, Object> resultado =
                servicio.ejecutarMochilaDp(depositoId, vehiculoId, destino);
        return ResponseEntity.ok(resultado);
    }
}
