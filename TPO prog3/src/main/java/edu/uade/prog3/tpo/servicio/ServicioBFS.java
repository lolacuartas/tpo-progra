// src/main/java/edu/uade/prog3/tpo/servicio/ServicioBFS.java
package edu.uade.prog3.tpo.servicio;

import edu.uade.prog3.tpo.dominio.ResultadoBFS;
import edu.uade.prog3.tpo.repositorio.IRepositorioGrafo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServicioBFS {

    private final IRepositorioGrafo repo;

    public ServicioBFS(IRepositorioGrafo repo) {
        this.repo = repo;
    }

    /**
     * BFS genérico sobre el grafo:
     * - Si incluirClientes=false: recorre sólo Hubs via :RUTA
     * - Si incluirClientes=true: también permite hops por :ATIENDE hacia/desde Cliente
     *
     * Recomendado: usar IDs reales (DEP_*, SUC_*, CLI_*).
     */
    public ResultadoBFS bfs(String origenId, String destinoId, Integer maxProfundidad, boolean incluirClientes) {
        validarExistencia(origenId, incluirClientes);
        validarExistencia(destinoId, incluirClientes);

        // Caso trivial
        if (origenId.equals(destinoId)) {
            return construirResultado(origenId, destinoId, List.of(origenId));
        }

        // BFS
        Deque<String> cola = new ArrayDeque<>();
        Set<String> visitados = new HashSet<>();
        Map<String, String> padre = new HashMap<>();
        Map<String, Integer> nivel = new HashMap<>();

        cola.add(origenId);
        visitados.add(origenId);
        nivel.put(origenId, 0);

        while (!cola.isEmpty()) {
            String actual = cola.removeFirst();
            int nl = nivel.getOrDefault(actual, 0);

            if (maxProfundidad != null && maxProfundidad > 0 && nl >= maxProfundidad) {
                continue;
            }

            List<String> vecinos = incluirClientes
                    ? repo.vecinosIncluyendoClientes(actual)
                    : repo.vecinosHubs(actual);

            for (String v : vecinos) {
                if (visitados.contains(v)) continue;

                visitados.add(v);
                padre.put(v, actual);
                nivel.put(v, nl + 1);

                if (v.equals(destinoId)) {
                    List<String> camino = reconstruirCamino(padre, origenId, destinoId);
                    return construirResultado(origenId, destinoId, camino);
                }
                cola.addLast(v);
            }
        }
        // No hay camino
        return construirResultado(origenId, destinoId, List.of());
    }

    private void validarExistencia(String id, boolean incluirClientes) {
        boolean existe = repo.existeHubPorId(id) || (incluirClientes && repo.existeClientePorId(id));
        if (!existe) {
            throw new IllegalArgumentException("No existe un nodo Hub/Cliente con id=" + id);
        }
    }

    private List<String> reconstruirCamino(Map<String, String> padre, String origen, String destino) {
        LinkedList<String> camino = new LinkedList<>();
        String cur = destino;
        while (cur != null) {
            camino.addFirst(cur);
            if (cur.equals(origen)) break;
            cur = padre.get(cur);
        }
        if (!camino.isEmpty() && camino.getFirst().equals(origen)) return camino;
        return List.of();
    }

    private ResultadoBFS construirResultado(String origen, String destino, List<String> caminoIds) {
        List<String> nombres = caminoIds.stream()
                .map(id -> repo.nombreDe(id).orElse(id))
                .toList();
        return new ResultadoBFS(origen, destino, caminoIds, nombres);
    }

    /**
     * Atajo de negocio: si destino es Cliente, resolvemos BFS hasta su Sucursal y agregamos el cliente.
     * Útil cuando querés que el recorrido “real” termine en la sucursal y el cliente sea un apéndice.
     */
    public ResultadoBFS bfsHastaClienteViaSucursal(String origenId, String clienteId, Integer maxProfundidad) {
        String sucursal = repo.sucursalQueAtiendeCliente(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("El cliente " + clienteId + " no está atendido por ninguna sucursal"));
        ResultadoBFS r = bfs(origenId, sucursal, maxProfundidad, false);
        if (r.getCaminoIds().isEmpty()) return r;
        // anexamos el cliente al final, sin aumentar aristas de RUTA
        List<String> caminoIds = new ArrayList<>(r.getCaminoIds());
        caminoIds.add(clienteId);
        return new ResultadoBFS(origenId, clienteId, caminoIds,
                caminoIds.stream().map(id -> repo.nombreDe(id).orElse(id)).toList());
    }
}
