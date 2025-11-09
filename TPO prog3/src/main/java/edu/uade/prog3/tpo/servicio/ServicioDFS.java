// src/main/java/edu/uade/prog3/tpo/servicio/ServicioDFS.java
package edu.uade.prog3.tpo.servicio;

import edu.uade.prog3.tpo.dominio.ResultadoDFS;
import edu.uade.prog3.tpo.repositorio.IRepositorioGrafo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServicioDFS {

    private final IRepositorioGrafo repo;

    public ServicioDFS(IRepositorioGrafo repo) { this.repo = repo; }

    /**
     * DFS para encontrar UN camino de origen a destino.
     * - incluirClientes=false: recorre solo :RUTA entre Hubs
     * - incluirClientes=true : agrega hops por :ATIENDE Sucursal<->Cliente
     * - maxProfundidad: si null/<=0, sin límite razonable (controlado con visited)
     */
    public ResultadoDFS dfsCamino(String origenId, String destinoId,
                                  Integer maxProfundidad, boolean incluirClientes) {
        validarExistencia(origenId, incluirClientes);
        validarExistencia(destinoId, incluirClientes);

        if (origenId.equals(destinoId)) {
            var nombres = repo.nombresDe(List.of(origenId));
            return new ResultadoDFS(origenId, destinoId, List.of(origenId), nombres, List.of(), List.of());
        }

        Set<String> visitados = new HashSet<>();
        Deque<String> pila = new ArrayDeque<>();
        Map<String, String> padre = new HashMap<>();
        Map<String, Integer> nivel = new HashMap<>();
        List<String> orden = new ArrayList<>(); // <--- lista para mostrar el orden del recorrido

        pila.push(origenId);
        nivel.put(origenId, 0);

        while (!pila.isEmpty()) {
            String actual = pila.pop();
            if (!visitados.add(actual)) continue;

            orden.add(actual); // <--- agregamos el nodo al orden de visita

            if (actual.equals(destinoId)) {
                List<String> camino = reconstruirCamino(padre, origenId, destinoId);
                return new ResultadoDFS(origenId, destinoId, camino, repo.nombresDe(camino), orden, repo.nombresDe(orden));
            }

            int nl = nivel.getOrDefault(actual, 0);
            if (maxProfundidad != null && maxProfundidad > 0 && nl >= maxProfundidad) {
                continue; // no expandimos más este nodo
            }

            List<String> vecinos = incluirClientes
                    ? repo.vecinosIncluyendoClientes(actual)
                    : vecinosSoloHubs(actual);

            // Para que el recorrido sea más "determinista" respecto del orden,
            // apilamos en orden inverso (LIFO) para visitar alfabéticamente.
            ListIterator<String> it = vecinos.listIterator(vecinos.size());
            while (it.hasPrevious()) {
                String v = it.previous();
                if (!visitados.contains(v)) {
                    if (!nivel.containsKey(v)) nivel.put(v, nl + 1);
                    if (!padre.containsKey(v)) padre.put(v, actual);
                    pila.push(v);
                }
            }
        }
        // No hay camino
        return new ResultadoDFS(origenId, destinoId, List.of(), List.of(), List.of(), List.of());
    }

    /**
     * DFS recorrido (preorden) desde un origen.
     * Devuelve el orden de visita; útil para exploración y pruebas.
     */
    public ResultadoDFS dfsRecorrido(String origenId, Integer maxProfundidad, boolean incluirClientes) {
        validarExistencia(origenId, incluirClientes);

        Set<String> visitados = new HashSet<>();
        Deque<String> pila = new ArrayDeque<>();
        Map<String, Integer> nivel = new HashMap<>();
        List<String> orden = new ArrayList<>();

        pila.push(origenId);
        nivel.put(origenId, 0);

        while (!pila.isEmpty()) {
            String actual = pila.pop();
            if (!visitados.add(actual)) continue;

            orden.add(actual);

            int nl = nivel.getOrDefault(actual, 0);
            if (maxProfundidad != null && maxProfundidad > 0 && nl >= maxProfundidad) {
                continue;
            }

            List<String> vecinos = incluirClientes
                    ? repo.vecinosIncluyendoClientes(actual)
                    : vecinosSoloHubs(actual);

            ListIterator<String> it = vecinos.listIterator(vecinos.size());
            while (it.hasPrevious()) {
                String v = it.previous();
                if (!visitados.contains(v)) {
                    if (!nivel.containsKey(v)) nivel.put(v, nl + 1);
                    pila.push(v);
                }
            }
        }

        return new ResultadoDFS(
                origenId, null,
                List.of(), List.of(),
                orden, repo.nombresDe(orden)
        );
    }

    // ---------- helpers ----------

    private void validarExistencia(String id, boolean incluirClientes) {
        boolean existe = repo.existeHubPorId(id) || (incluirClientes && existeCliente(id));
        if (!existe) throw new IllegalArgumentException("No existe Hub/Cliente con id=" + id);
    }

    private boolean existeCliente(String id) {
        try {
            // si no tenés este método público, añadilo en tu repo como en BFS
            var m = repo.getClass().getMethod("existeClientePorId", String.class);
            return (boolean) m.invoke(repo, id);
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> vecinosSoloHubs(String id) {
        // Si tu repo ya expone vecinosHubs(id) (como en BFS), usalo directo:
        return repo.vecinosHubs(id);
    }

    private static List<String> reconstruirCamino(Map<String, String> padre, String origen, String destino) {
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
}
