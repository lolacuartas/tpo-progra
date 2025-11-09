// src/main/java/edu/uade/prog3/tpo/api/ControladorBFS.java
package edu.uade.prog3.tpo.api;

import edu.uade.prog3.tpo.dominio.ResultadoBFS;
import edu.uade.prog3.tpo.servicio.ServicioBFS;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grafos")
public class ControladorBFS {

    private final ServicioBFS ServicioBFS;

    public ControladorBFS(ServicioBFS ServicioBFS) {
        this.ServicioBFS = ServicioBFS;
    }

    /**
     * BFS general:
     * - Recorre Hubs por :RUTA.
     * - Si incluirClientes=true, permite hops por :ATIENDE (Sucursal<->Cliente).
     *
     * Ejemplos:
     *  http://localhost:8080/api/grafos/bfs?origenId=DEP_SUR&destinoId=SUC_PALERMO
     *  http://localhost:8080/api/grafos/bfs?origenId=DEP_SUR&destinoId=CLI_PAL_2&incluirClientes=true
     */
    @GetMapping("/bfs")
    public ResponseEntity<?> bfs(
            @RequestParam String origenId,
            @RequestParam String destinoId,
            @RequestParam(required = false) Integer maxProfundidad,
            @RequestParam(defaultValue = "false") boolean incluirClientes
    ) {
        try {
            ResultadoBFS resultado = ServicioBFS.bfs(origenId, destinoId, maxProfundidad, incluirClientes);
            if (resultado.getCaminoIds().isEmpty()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Atajo de negocio: si el destino es un Cliente, resolvemos BFS hasta la Sucursal que lo atiende
     * y anexamos el cliente como último nodo (sin recorrer :ATIENDE durante el BFS).
     *
     * Ejemplo:
     *  /api/grafos/bfs-hasta-cliente?origenId=DEP_SUR&clienteId=CLI_PAL_2
     */
    @GetMapping("/bfs-hasta-cliente")
    public ResponseEntity<?> bfsHastaCliente(
            @RequestParam String origenId,
            @RequestParam String clienteId,
            @RequestParam(required = false) Integer maxProfundidad
    ) {
        try {
            ResultadoBFS resultado = ServicioBFS.bfsHastaClienteViaSucursal(origenId, clienteId, maxProfundidad);
            if (resultado.getCaminoIds().isEmpty()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
