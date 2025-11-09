// src/main/java/edu/uade/prog3/tpo/dominio/ResultadoDFS.java
package edu.uade.prog3.tpo.dominio;

import java.util.List;

public class ResultadoDFS {
    private final String origenId;
    private final String destinoId;          // null si es recorrido
    private final List<String> caminoIds;    // para DFS camino (vacío si no se encontró)
    private final List<String> caminoNombres;
    private final List<String> recorridoIds; // orden de visita en preorden (para recorrido)
    private final List<String> recorridoNombres;

    public ResultadoDFS(String origenId, String destinoId,
                        List<String> caminoIds, List<String> caminoNombres,
                        List<String> recorridoIds, List<String> recorridoNombres) {
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.caminoIds = caminoIds;
        this.caminoNombres = caminoNombres;
        this.recorridoIds = recorridoIds;
        this.recorridoNombres = recorridoNombres;
    }
    public String getOrigenId() { return origenId; }
    public String getDestinoId() { return destinoId; }
    public List<String> getCaminoIds() { return caminoIds; }
    public List<String> getCaminoNombres() { return caminoNombres; }
    public List<String> getRecorridoIds() { return recorridoIds; }
    public List<String> getRecorridoNombres() { return recorridoNombres; }
}
