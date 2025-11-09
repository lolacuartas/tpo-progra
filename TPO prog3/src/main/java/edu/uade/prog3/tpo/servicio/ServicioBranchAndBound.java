// src/main/java/edu/uade/prog3/tpo/servicio/ServicioBranchAndBound.java
package edu.uade.prog3.tpo.servicio;

import edu.uade.prog3.tpo.dominio.CriterioPeso;
import edu.uade.prog3.tpo.dominio.ResultadoDijkstra;
import edu.uade.prog3.tpo.repositorio.IRepositorioGrafo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServicioBranchAndBound {

    private final IRepositorioGrafo repo;
    private final ServicioDijkstra servicioDijkstra;

    public ServicioBranchAndBound(IRepositorioGrafo repo, ServicioDijkstra servicioDijkstra) {
        this.repo = repo;
        this.servicioDijkstra = servicioDijkstra;
    }

    /**
     * Busca la ruta óptima (mínimo costo total) que:
     * - parte del origen
     * - pasa por todas las paradas (en cualquier orden)
     * - llega al destino (hub o cliente)
     * Usa dist_km como criterio de costo.
     */
    public Map<String, Object> rutaOptima(String origenId,
                                          String destinoId,
                                          List<String> paradas) {

        // Validaciones
        if (!repo.existeHubPorId(origenId)) {
            throw new IllegalArgumentException("El origen debe ser un Hub válido: " + origenId);
        }

        boolean destinoEsCliente = repo.existeClientePorId(destinoId);
        boolean destinoEsHub = repo.existeHubPorId(destinoId);

        // Normalizamos paradas (deben ser hubs)
        List<String> paradasValidas = new ArrayList<>();
        if (paradas != null) {
            for (String p : paradas) {
                if (p == null || p.isBlank()) continue;
                String id = p.trim();
                if (!repo.existeHubPorId(id)) {
                    throw new IllegalArgumentException("La parada obligatoria no existe o no es Hub: " + id);
                }
                paradasValidas.add(id);
            }
        }

        if (destinoEsHub) {
            return resolverBranchAndBound(origenId, destinoId, paradasValidas);
        }

        if (destinoEsCliente) {
            List<String> sucs = repo.sucursalesQueAtiendenCliente(destinoId);
            if (sucs == null || sucs.isEmpty()) {
                throw new IllegalArgumentException("El cliente " + destinoId + " no es atendido por ninguna sucursal.");
            }

            Map<String, Object> mejor = null;
            double mejorCosto = Double.POSITIVE_INFINITY;

            for (String sucFinal : sucs) {
                Map<String, Object> parcial = resolverBranchAndBound(origenId, sucFinal, paradasValidas);
                if (parcial == null) continue;
                double costo = (double) parcial.get("costo");
                if (costo < mejorCosto) {
                    mejor = parcial;
                    mejorCosto = costo;
                }
            }

            if (mejor == null) {
                throw new IllegalStateException("No se encontró ruta que pase por las paradas y llegue a una sucursal que atienda al cliente " + destinoId);
            }

            @SuppressWarnings("unchecked")
            List<String> camino = (List<String>) mejor.get("camino");
            camino.add(destinoId);
            mejor.put("destino", destinoId);
            mejor.put("camino", camino);
            mejor.put("nombres", repo.nombresDe(camino));
            return mejor;
        }

        throw new IllegalArgumentException("No existe destino: " + destinoId);
    }

    /**
     * Algoritmo Branch & Bound: prueba combinaciones pero poda las ramas no prometedoras.
     */
    private Map<String, Object> resolverBranchAndBound(String origenId,
                                                       String destinoHubId,
                                                       List<String> paradas) {

        double mejorCosto = Double.POSITIVE_INFINITY;
        List<String> mejorCamino = new ArrayList<>();

        // Cola de estados a explorar (min-heap por costo estimado)
        PriorityQueue<Estado> frontera = new PriorityQueue<>(Comparator.comparingDouble(e -> e.costoEstimado));

        // Estado inicial
        frontera.add(new Estado(origenId, paradas, new ArrayList<>(List.of(origenId)), 0.0));

        while (!frontera.isEmpty()) {
            Estado actual = frontera.poll();

            // poda
            if (actual.costoAcumulado >= mejorCosto) continue;

            // si no quedan paradas, ir al destino final
            if (actual.paradasRestantes.isEmpty()) {
                ResultadoDijkstra hastaDestino = servicioDijkstra.dijkstra(actual.nodoActual, destinoHubId, CriterioPeso.DISTANCIA, 1.0);
                if (hastaDestino.getCaminoIds().isEmpty()) continue;

                double costoTotal = actual.costoAcumulado + hastaDestino.getPesoTotal();
                if (costoTotal < mejorCosto) {
                    mejorCosto = costoTotal;
                    List<String> nuevoCamino = new ArrayList<>(actual.camino);
                    List<String> tramo = hastaDestino.getCaminoIds();
                    for (int i = 1; i < tramo.size(); i++) nuevoCamino.add(tramo.get(i));
                    mejorCamino = nuevoCamino;
                }
                continue;
            }

            // expandir: elegir una parada siguiente y crear un nuevo estado
            for (String siguiente : actual.paradasRestantes) {
                ResultadoDijkstra hasta = servicioDijkstra.dijkstra(actual.nodoActual, siguiente, CriterioPeso.DISTANCIA, 1.0);
                if (hasta.getCaminoIds().isEmpty()) continue;

                double nuevoCosto = actual.costoAcumulado + hasta.getPesoTotal();
                if (nuevoCosto >= mejorCosto) continue; // poda por cota superior

                // estimación optimista (bound inferior): costo actual + mínima distancia a cualquier nodo restante
                double minHeuristica = estimarCotaInferior(siguiente, actual.paradasRestantes, destinoHubId);
                double costoEstimado = nuevoCosto + minHeuristica;

                List<String> nuevoCamino = new ArrayList<>(actual.camino);
                List<String> tramo = hasta.getCaminoIds();
                for (int i = 1; i < tramo.size(); i++) nuevoCamino.add(tramo.get(i));

                List<String> nuevasRestantes = new ArrayList<>(actual.paradasRestantes);
                nuevasRestantes.remove(siguiente);

                frontera.add(new Estado(siguiente, nuevasRestantes, nuevoCamino, nuevoCosto, costoEstimado));
            }
        }

        if (mejorCamino.isEmpty()) return null;

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("origen", origenId);
        resp.put("destino", destinoHubId);
        resp.put("obligatorias", paradas);
        resp.put("camino", mejorCamino);
        resp.put("nombres", repo.nombresDe(mejorCamino));
        resp.put("costo", mejorCosto);
        return resp;
    }

    /**
     * Estima una cota inferior (heurística): la distancia más corta entre
     * el nodo actual, las paradas restantes y el destino final.
     */
    private double estimarCotaInferior(String actual, List<String> restantes, String destinoFinal) {
        double min = Double.POSITIVE_INFINITY;
        List<String> candidatos = new ArrayList<>(restantes);
        candidatos.add(destinoFinal);

        for (String n : candidatos) {
            ResultadoDijkstra r = servicioDijkstra.dijkstra(actual, n, CriterioPeso.DISTANCIA, 1.0);
            if (!r.getCaminoIds().isEmpty()) {
                min = Math.min(min, r.getPesoTotal());
            }
        }
        return min == Double.POSITIVE_INFINITY ? 0 : min;
    }

    // Estado interno de la búsqueda
    private static class Estado {
        String nodoActual;
        List<String> paradasRestantes;
        List<String> camino;
        double costoAcumulado;
        double costoEstimado; // para el bound

        public Estado(String nodoActual, List<String> paradasRestantes, List<String> camino, double costoAcumulado) {
            this(nodoActual, paradasRestantes, camino, costoAcumulado, costoAcumulado);
        }

        public Estado(String nodoActual, List<String> paradasRestantes, List<String> camino,
                      double costoAcumulado, double costoEstimado) {
            this.nodoActual = nodoActual;
            this.paradasRestantes = paradasRestantes;
            this.camino = camino;
            this.costoAcumulado = costoAcumulado;
            this.costoEstimado = costoEstimado;
        }
    }
}
