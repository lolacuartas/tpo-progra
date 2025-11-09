// src/main/java/edu/uade/prog3/tpo/dominio/AristaRuta.java
package edu.uade.prog3.tpo.dominio;

public class AristaRuta {

    private final String desdeId;
    private final String hastaId;
    private final double distKm;
    private final double tiempoMin;
    private final double peaje;

    public AristaRuta(String desdeId, String hastaId, double distKm, double tiempoMin, double peaje) {
        this.desdeId = desdeId;
        this.hastaId = hastaId;
        this.distKm = distKm;
        this.tiempoMin = tiempoMin;
        this.peaje = peaje;
    }

    public String getDesdeId() { return desdeId; }
    public String getHastaId() { return hastaId; }
    public double getDistKm() { return distKm; }
    public double getTiempoMin() { return tiempoMin; }
    public double getPeaje() { return peaje; }
}
