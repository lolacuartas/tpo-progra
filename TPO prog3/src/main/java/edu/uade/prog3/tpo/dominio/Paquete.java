package edu.uade.prog3.tpo.dominio;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Paquete")
public class Paquete {

    @Id
    private String id;

    private Double peso_kg;
    private Double volumen_m3;
    private Integer prioridad;   // 1 = alta, 2 = media, 3 = baja
    private String estado;       // pendiente, asignado, entregado, etc.

    public Paquete() {}

    public Paquete(String id, Double peso_kg, Double volumen_m3, Integer prioridad, String estado) {
        this.id = id;
        this.peso_kg = peso_kg;
        this.volumen_m3 = volumen_m3;
        this.prioridad = prioridad;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public Double getPeso_kg() {
        return peso_kg;
    }

    public Double getVolumen_m3() {
        return volumen_m3;
    }

    public Integer getPrioridad() {
        return prioridad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
