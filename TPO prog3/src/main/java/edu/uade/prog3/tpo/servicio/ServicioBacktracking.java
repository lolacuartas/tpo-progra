// src/main/java/edu/uade/prog3/tpo/servicio/ServicioBacktracking.java
package edu.uade.prog3.tpo.servicio;

import edu.uade.prog3.tpo.dominio.CriterioPeso;
import edu.uade.prog3.tpo.dominio.ResultadoDijkstra;
import edu.uade.prog3.tpo.repositorio.IRepositorioGrafo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServicioBacktracking {

    private final IRepositorioGrafo repo;
    private final ServicioDijkstra servicioDijkstra;

    public ServicioBacktracking(IRepositorioGrafo repo,
                                ServicioDijkstra servicioDijkstra) {
        this.repo = repo;
        this.servicioDijkstra = servicioDijkstra;
    }

    /**
     * Busca una ruta que:
     *  - salga de origen
     *  - visite TODAS las paradas (en cualquier orden)
     *  - termine en destino (hub o cliente)
     *
     * Usa backtracking sobre las paradas + Dijkstra entre ellas.
     */
    public Map<String, Object> rutaConParadas(String origenId,
                                              String destinoId,
                                              List<String> paradasObligatorias) {

        if (!repo.existeHubPorId(origenId)) {
            throw new IllegalArgumentException("El origen debe ser un Hub válido: " + origenId);
        }

        // Normalizamos paradas (todas deben existir como Hubs)
        List<String> paradas = new ArrayList<>();
        if (paradasObligatorias != null) {
            for (String p : paradasObligatorias) {
                if (p == null || p.isBlank()) continue;
                String pp = p.trim();
                if (!repo.existeHubPorId(pp)) {
                    throw new IllegalArgumentException("La parada obligatoria no existe o no es Hub: " + pp);
                }
                paradas.add(pp);
            }
        }

        boolean destinoEsCliente = repo.existeClientePorId(destinoId);
        boolean destinoEsHub = repo.existeHubPorId(destinoId);

        // Caso 1: destino es HUB
        if (destinoEsHub) {
            return resolverBacktracking(origenId, destinoId, paradas);
        }

        // Caso 2: destino es CLIENTE
        if (destinoEsCliente) {
            List<String> sucs = repo.sucursalesQueAtiendenCliente(destinoId);
            if (sucs == null || sucs.isEmpty()) {
                throw new IllegalArgumentException("El cliente " + destinoId + " no es atendido por ninguna sucursal.");
            }

            Map<String, Object> mejor = null;
            double mejorCosto = Double.POSITIVE_INFINITY;

            for (String sucFinal : sucs) {
                Map<String, Object> res = resolverBacktracking(origenId, sucFinal, paradas);
                if (res == null) continue;
                double costo = (double) res.get("costo");
                if (costo < mejorCosto) {
                    mejor = res;
                    mejorCosto = costo;
                }
            }

            if (mejor == null) {
                throw new IllegalStateException("No se encontró ruta que pase por las paradas y llegue a una sucursal que atienda al cliente " + destinoId);
            }

            // Agrego el cliente al final del camino
            @SuppressWarnings("unchecked")
            List<String> camino = (List<String>) mejor.get("camino");
            List<String> caminoFinal = new ArrayList<>(camino);
            caminoFinal.add(destinoId);

            // Agrego nombres
            List<String> nombresFinal = repo.nombresDe(caminoFinal);

            mejor.put("destino", destinoId);
            mejor.put("camino", caminoFinal);
            mejor.put("nombres", nombresFinal);
            return mejor;
        }

        throw new IllegalArgumentException("No existe destino: " + destinoId);
    }

    /**
     * Backtracking sobre las PARADAS:
     * prueba todas las permutaciones de orden de visita
     * y usa Dijkstra para calcular cada tramo.
     */
    private Map<String, Object> resolverBacktracking(String origenId,
                                                     String destinoHubId,
                                                     List<String> paradas) {

        if (paradas.isEmpty()) {
            ResultadoDijkstra fin = servicioDijkstra.dijkstra(origenId, destinoHubId, CriterioPeso.DISTANCIA, 1.0);
            if (fin.getCaminoIds().isEmpty()) return null;

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("origen", origenId);
            resp.put("destino", destinoHubId);
            resp.put("obligatorias", List.of());
            resp.put("camino", fin.getCaminoIds());
            resp.put("nombres", repo.nombresDe(fin.getCaminoIds()));  // 🔹 NOMBRES
            resp.put("costo", fin.getPesoTotal());
            return resp;
        }

        double[] mejorCosto = {Double.POSITIVE_INFINITY};
        List<String> mejorCamino = new ArrayList<>();
        backtrackParadas(origenId, destinoHubId, paradas,
                new ArrayList<>(), new ArrayList<>(), 0.0,
                mejorCosto, mejorCamino, 0);

        if (mejorCosto[0] == Double.POSITIVE_INFINITY) {
            return null;
        }

        // Agregamos también los nombres del camino
        List<String> nombres = repo.nombresDe(mejorCamino);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("origen", origenId);
        resp.put("destino", destinoHubId);
        resp.put("obligatorias", paradas);
        resp.put("camino", mejorCamino);
        resp.put("nombres", nombres);
        resp.put("costo", mejorCosto[0]);
        return resp;
    }

    private void backtrackParadas(String actual,
                                  String destinoFinal,
                                  List<String> faltan,
                                  List<String> ordenVisitado,
                                  List<String> caminoAcumulado,
                                  double costoAcumulado,
                                  double[] mejorCosto,
                                  List<String> mejorCamino,
                                  int profundidad) {

        if (profundidad > 15) return;
        if (costoAcumulado >= mejorCosto[0]) return;

        // caso base: ya no quedan paradas → voy al destino
        if (faltan.isEmpty()) {
            ResultadoDijkstra hastaDestino = servicioDijkstra.dijkstra(actual, destinoFinal, CriterioPeso.DISTANCIA, 1.0);
            if (hastaDestino.getCaminoIds().isEmpty()) return;

            double costoFinal = costoAcumulado + hastaDestino.getPesoTotal();
            if (costoFinal < mejorCosto[0]) {
                mejorCosto[0] = costoFinal;
                List<String> nuevoCamino = new ArrayList<>(caminoAcumulado);
                List<String> tramo = hastaDestino.getCaminoIds();
                for (int i = 1; i < tramo.size(); i++) nuevoCamino.add(tramo.get(i));
                mejorCamino.clear();
                mejorCamino.addAll(nuevoCamino);
            }
            return;
        }

        // probar cada parada como siguiente
        for (int i = 0; i < faltan.size(); i++) {
            String siguiente = faltan.get(i);
            ResultadoDijkstra hasta = servicioDijkstra.dijkstra(actual, siguiente, CriterioPeso.DISTANCIA, 1.0);
            if (hasta.getCaminoIds().isEmpty()) continue;

            double nuevoCosto = costoAcumulado + hasta.getPesoTotal();
            if (nuevoCosto >= mejorCosto[0]) continue;

            List<String> nuevoCaminoAcum = new ArrayList<>(caminoAcumulado);
            List<String> tramo = hasta.getCaminoIds();
            if (caminoAcumulado.isEmpty()) nuevoCaminoAcum.addAll(tramo);
            else for (int j = 1; j < tramo.size(); j++) nuevoCaminoAcum.add(tramo.get(j));

            List<String> nuevaFaltan = new ArrayList<>(faltan);
            nuevaFaltan.remove(i);

            backtrackParadas(siguiente, destinoFinal, nuevaFaltan, ordenVisitado,
                    nuevoCaminoAcum, nuevoCosto, mejorCosto, mejorCamino, profundidad + 1);
        }
    }
}
