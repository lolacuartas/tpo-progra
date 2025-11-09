
package edu.uade.prog3.tpo.dominio;

import java.util.List;
import java.util.Map;

public class ResultadoDijkstra {
    private final String origenId;
    private final String destinoId;
    private final List<String> caminoIds;
    private final List<String> caminoNombres;
    private final List<AristaRuta> aristas; // detalle tramo a tramo
    private final double pesoTotal;         // según el criterio elegido
    private final CriterioPeso criterio;
    private final Map<String, Double> acumulados; // dist a cada nodo (debug/explicativo)

    public ResultadoDijkstra(String origenId, String destinoId, List<String> caminoIds,
                             List<String> caminoNombres, List<AristaRuta> aristas,
                             double pesoTotal, CriterioPeso criterio, Map<String, Double> acumulados) {
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.caminoIds = caminoIds;
        this.caminoNombres = caminoNombres;
        this.aristas = aristas;
        this.pesoTotal = pesoTotal;
        this.criterio = criterio;
        this.acumulados = acumulados;
    }

    public String getOrigenId() { return origenId; }
    public String getDestinoId() { return destinoId; }
    public List<String> getCaminoIds() { return caminoIds; }
    public List<String> getCaminoNombres() { return caminoNombres; }
    public List<AristaRuta> getAristas() { return aristas; }
    public double getPesoTotal() { return pesoTotal; }
    public CriterioPeso getCriterio() { return criterio; }
    public Map<String, Double> getAcumulados() { return acumulados; }
}
