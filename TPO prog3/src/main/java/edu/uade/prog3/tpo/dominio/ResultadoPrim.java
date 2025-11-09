// src/main/java/edu/uade/prog3/tpo/dominio/ResultadoPrim.java
package edu.uade.prog3.tpo.dominio;

import java.util.List;

public class ResultadoPrim {
    private final String inicioId;
    private final List<AristaRuta> aristas;
    private final double pesoTotal;
    private final List<String> nodos;
    private final String criterio; // "DISTANCIA" o "TIEMPO" o "COSTO"

    public ResultadoPrim(String inicioId, List<AristaRuta> aristas,
                         double pesoTotal, List<String> nodos, String criterio) {
        this.inicioId = inicioId;
        this.aristas = aristas;
        this.pesoTotal = pesoTotal;
        this.nodos = nodos;
        this.criterio = criterio;
    }

    public String getInicioId() { return inicioId; }
    public List<AristaRuta> getAristas() { return aristas; }
    public double getPesoTotal() { return pesoTotal; }
    public List<String> getNodos() { return nodos; }
    public String getCriterio() { return criterio; }
}
