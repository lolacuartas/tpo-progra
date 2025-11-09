// src/main/java/edu/uade/prog3/tpo/dominio/ResultadoBFS.java
package edu.uade.prog3.tpo.dominio;

import java.util.List;

public class ResultadoBFS {
    private final String origenId;
    private final String destinoId;
    private final List<String> caminoIds;      // IDs recorridos
    private final List<String> caminoNombres;  // Nombres útiles para mostrar
    private final int longitud;                // cantidad de aristas

    public ResultadoBFS(String origenId, String destinoId, List<String> caminoIds, List<String> caminoNombres) {
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.caminoIds = caminoIds;
        this.caminoNombres = caminoNombres;
        this.longitud = (caminoIds == null || caminoIds.size() < 2) ? 0 : caminoIds.size() - 1;
    }
    public String getOrigenId() { return origenId; }
    public String getDestinoId() { return destinoId; }
    public List<String> getCaminoIds() { return caminoIds; }
    public List<String> getCaminoNombres() { return caminoNombres; }
    public int getLongitud() { return longitud; }
}
