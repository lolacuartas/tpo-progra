package edu.uade.prog3.tpo.servicio;

import edu.uade.prog3.tpo.dominio.AristaRuta;
import edu.uade.prog3.tpo.dominio.CriterioPeso;
import edu.uade.prog3.tpo.dominio.ResultadoDijkstra;
import edu.uade.prog3.tpo.repositorio.IRepositorioGrafo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServicioDijkstra {

    private final IRepositorioGrafo repo;

    public ServicioDijkstra(IRepositorioGrafo repo) {
        this.repo = repo;
    }

    /**
     * Ahora soporta:
     * - destino HUB (como siempre)
     * - destino CLIENTE (elige la sucursal que lo atiende con menor costo)
     */
    public ResultadoDijkstra dijkstra(String origenId, String destinoId,
                                      CriterioPeso criterio, double factorPeaje) {

        boolean destinoEsCliente = repo.existeClientePorId(destinoId);
        boolean destinoEsHub = repo.existeHubPorId(destinoId);

        // origen siempre es hub
        validarHub(origenId);

        if (destinoEsHub) {
            return dijkstraSoloHubs(origenId, destinoId, criterio, factorPeaje);
        }

        if (destinoEsCliente) {
            return dijkstraHastaCliente(origenId, destinoId, criterio, factorPeaje);
        }

        throw new IllegalArgumentException("No existe destino con id=" + destinoId);
    }

    // =========================================================
    //  CASO 1: HUB → HUB  (lo que ya tenías)
    // =========================================================
    private ResultadoDijkstra dijkstraSoloHubs(String origenId, String destinoId,
                                               CriterioPeso criterio, double factorPeaje) {

        if (origenId.equals(destinoId)) {
            return new ResultadoDijkstra(
                    origenId,
                    destinoId,
                    List.of(origenId),
                    repo.nombresDe(List.of(origenId)),
                    List.of(),
                    0.0,
                    criterio,
                    Map.of(origenId, 0.0)
            );
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> padre = new HashMap<>();
        Map<String, AristaRuta> aristaTomada = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        Set<String> visitados = new HashSet<>();

        dist.put(origenId, 0.0);
        pq.add(origenId);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (!visitados.add(u)) continue;
            if (u.equals(destinoId)) break;

            for (var row : repo.vecinosConPesos(u)) {
                String v = (String) row.get("vecino");
                double dKm = toDouble(row.get("dist"));
                double tMin = toDouble(row.get("tiempo"));
                double peaje = toDouble(row.get("peaje"));

                double w = peso(dKm, tMin, peaje, criterio, factorPeaje);
                double alt = dist.getOrDefault(u, Double.POSITIVE_INFINITY) + w;

                if (alt < dist.getOrDefault(v, Double.POSITIVE_INFINITY)) {
                    dist.put(v, alt);
                    padre.put(v, u);
                    aristaTomada.put(v, new AristaRuta(u, v, dKm, tMin, peaje));
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }

        List<String> camino = reconstruirCamino(padre, origenId, destinoId);
        if (camino.isEmpty()) {
            return new ResultadoDijkstra(
                    origenId,
                    destinoId,
                    List.of(),
                    List.of(),
                    List.of(),
                    Double.POSITIVE_INFINITY,
                    criterio,
                    dist
            );
        }

        List<AristaRuta> aristas = reconstruirAristas(aristaTomada, camino);
        double total = dist.getOrDefault(destinoId, Double.POSITIVE_INFINITY);
        List<String> nombres = repo.nombresDe(camino);

        return new ResultadoDijkstra(
                origenId,
                destinoId,
                camino,
                nombres,
                aristas,
                total,
                criterio,
                dist
        );
    }

    // =========================================================
    //  CASO 2: HUB → CLIENTE
    // =========================================================
    private ResultadoDijkstra dijkstraHastaCliente(String origenId, String clienteId,
                                                   CriterioPeso criterio, double factorPeaje) {

        // 1) todas las sucursales que atienden a ese cliente
        List<String> sucursales = repo.sucursalesQueAtiendenCliente(clienteId);
        if (sucursales.isEmpty()) {
            throw new IllegalArgumentException("El cliente " + clienteId + " no es atendido por ninguna sucursal");
        }

        ResultadoDijkstra mejor = null;

        // 2) probamos llegar a cada sucursal y nos quedamos con la de menor pesoTotal
        for (String suc : sucursales) {
            ResultadoDijkstra parcial = dijkstraSoloHubs(origenId, suc, criterio, factorPeaje);
            // OJO: tu clase se llama getPesoTotal(), no getDistancia()
            if (mejor == null || parcial.getPesoTotal() < mejor.getPesoTotal()) {
                mejor = parcial;
            }
        }

        if (mejor == null || mejor.getCaminoIds() == null || mejor.getCaminoIds().isEmpty()) {
            throw new IllegalStateException(
                    "No hay camino desde " + origenId + " hacia las sucursales que atienden al cliente " + clienteId
            );
        }

        // 3) armar camino final: hubs + cliente
        List<String> caminoFinal = new ArrayList<>(mejor.getCaminoIds());
        caminoFinal.add(clienteId);

        // 4) nombres: los que ya tenías + nombre del cliente
        List<String> nombresFinal = new ArrayList<>(mejor.getCaminoNombres());
        repo.nombreDe(clienteId).ifPresent(nombresFinal::add);

        // 5) aristas: las mismas + una arista “virtual” sucursal→cliente
        List<AristaRuta> aristasFinal = new ArrayList<>(mejor.getAristas());
        String ultimaSucursal = mejor.getCaminoIds().get(mejor.getCaminoIds().size() - 1);
        aristasFinal.add(new AristaRuta(ultimaSucursal, clienteId, 0.0, 0.0, 0.0));

        // 6) devolvemos mismo tipo de objeto
        return new ResultadoDijkstra(
                origenId,
                clienteId,
                caminoFinal,
                nombresFinal,
                aristasFinal,
                mejor.getPesoTotal(),    // si querés podés sumar 0 o 1 acá
                criterio,
                mejor.getAcumulados()
        );
    }

    // =========================================================
    // helpers
    // =========================================================
    private void validarHub(String id) {
        if (!repo.existeHubPorId(id)) {
            throw new IllegalArgumentException("No existe Hub con id=" + id);
        }
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }

    private static double peso(double distKm, double tiempoMin, double peaje,
                               CriterioPeso criterio, double factorPeaje) {
        return switch (criterio) {
            case DISTANCIA -> distKm;
            case TIEMPO -> tiempoMin;
            case COSTO -> peaje * factorPeaje;
        };
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

    private static List<AristaRuta> reconstruirAristas(Map<String, AristaRuta> aristaTomada, List<String> camino) {
        if (camino.size() < 2) return List.of();
        List<AristaRuta> res = new ArrayList<>();
        for (int i = 1; i < camino.size(); i++) {
            String v = camino.get(i);
            AristaRuta a = aristaTomada.get(v);
            if (a != null) res.add(a);
        }
        return res;
    }
}
