package edu.uade.prog3.tpo.dominio;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Vehiculo")
public class Vehiculo {

    @Id
    private String id;

    private String patente;
    private Double capacidad_kg;
    private Double volumen_m3;
    private Double costo_km;
    private String tipo;

    public Vehiculo() {}

    public Vehiculo(String id, String patente, Double capacidad_kg, Double volumen_m3, Double costo_km, String tipo) {
        this.id = id;
        this.patente = patente;
        this.capacidad_kg = capacidad_kg;
        this.volumen_m3 = volumen_m3;
        this.costo_km = costo_km;
        this.tipo = tipo;
    }

    public String getId() {
        return id;
    }

    public String getPatente() {
        return patente;
    }

    public Double getCapacidad_kg() {
        return capacidad_kg;
    }

    public Double getVolumen_m3() {
        return volumen_m3;
    }

    public Double getCosto_km() {
        return costo_km;
    }

    public String getTipo() {
        return tipo;
    }
}
