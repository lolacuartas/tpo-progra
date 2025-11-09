package edu.uade.prog3.tpo.servicio;

import edu.uade.prog3.tpo.dominio.AristaRuta;
import edu.uade.prog3.tpo.dominio.CriterioPeso;
import edu.uade.prog3.tpo.dominio.ResultadoPrim;
import edu.uade.prog3.tpo.repositorio.IRepositorioGrafo;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Algoritmo de Prim sobre los nodos :Hub conectados por :RUTA.
 * Permite filtrar por un subconjunto de hubs (zona) pasando la lista de ids.
 */
@Service
public class ServicioPrim {

    private final IRepositorioGrafo repo;

    public ServicioPrim(IRepositorioGrafo repo) {
        this.repo = repo;
    }

    /**
     * @param inicioId     Hub de inicio (tiene que estar en la zona)
     * @param criterio     DISTANCIA | TIEMPO | COSTO
     * @param factorPeaje  multiplicador cuando criterio=COSTO
     * @param idsPermitidos lista de hubs sobre los que queremos construir el MST.
     *                      Si es null o vacía, usa TODOS los :Hub del grafo.
     */
    public ResultadoPrim prim(String inicioId,
                              CriterioPeso criterio,
                              double factorPeaje,
                              List<String> idsPermitidos) {

        // 1. validar que existe
        if (!repo.existeHubPorId(inicioId)) {
            throw new IllegalArgumentException("No existe Hub con id=" + inicioId);
        }

        // 2. armar universo de trabajo (la “zona”)
        List<String> zona = (idsPermitidos == null || idsPermitidos.isEmpty())
                ? repo.todosLosHubs()
                : idsPermitidos;

        if (!zona.contains(inicioId)) {
            throw new IllegalArgumentException("El inicio " + inicioId + " no está dentro de la zona indicada.");
        }

        // 3. estructura MST
        Set<String> enMST = new HashSet<>();
        enMST.add(inicioId);

        PriorityQueue<Frontera> frontera =
                new PriorityQueue<>(Comparator.comparingDouble(Frontera::peso));

        List<AristaRuta> aristasElegidas = new ArrayList<>();

        // 4. inicializamos la frontera con lo que sale del inicio
        agregarFrontera(inicioId, criterio, factorPeaje, enMST, frontera, zona);

        // 5. loop principal
        while (enMST.size() < zona.size() && !frontera.isEmpty()) {
            Frontera f = frontera.poll();

            // puede pasar que ese hastaId ya haya entrado por otra arista mejor
            if (enMST.contains(f.hastaId)) {
                continue;
            }

            enMST.add(f.hastaId);
            aristasElegidas.add(f.arista);

            // expandimos desde el nuevo nodo
            agregarFrontera(f.hastaId, criterio, factorPeaje, enMST, frontera, zona);
        }

        // 6. peso total según criterio
        double total = aristasElegidas.stream()
                .mapToDouble(a -> peso(a, criterio, factorPeaje))
                .sum();

        List<String> nodos = new ArrayList<>(enMST);
        nodos.sort(String::compareTo);

        return new ResultadoPrim(inicioId, aristasElegidas, total, nodos, criterio.name());
    }

    /**
     * Agrega a la priority queue todas las aristas salientes de 'desdeId'
     * que vayan a nodos NO visitados y que además estén dentro de la zona.
     */
    private void agregarFrontera(String desdeId,
                                 CriterioPeso criterio,
                                 double factorPeaje,
                                 Set<String> enMST,
                                 PriorityQueue<Frontera> frontera,
                                 List<String> zona) {

        // Traemos las RUTAs salientes con pesos
        for (var row : repo.vecinosConPesos(desdeId)) {
            String hastaId = (String) row.get("vecino");

            // filtrar por zona
            if (!zona.contains(hastaId)) continue;
            // filtrar los ya metidos en el MST
            if (enMST.contains(hastaId)) continue;

            double dist = toDouble(row.get("dist"));
            double tiempo = toDouble(row.get("tiempo"));
            double peaje = toDouble(row.get("peaje"));

            AristaRuta arista = new AristaRuta(desdeId, hastaId, dist, tiempo, peaje);
            double p = peso(arista, criterio, factorPeaje);

            frontera.add(new Frontera(p, hastaId, arista));
        }
    }

    /**
     * Calcula el peso según el criterio elegido.
     */
    private static double peso(AristaRuta a, CriterioPeso criterio, double factorPeaje) {
        return switch (criterio) {
            case DISTANCIA -> a.getDistKm();
            case TIEMPO -> a.getTiempoMin();
            case COSTO -> a.getPeaje() * factorPeaje;
        };
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }

    /**
     * Wrapper interno para la priority queue
     */
    private record Frontera(double peso, String hastaId, AristaRuta arista) {}
}
