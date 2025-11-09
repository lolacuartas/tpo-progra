// src/main/java/edu/uade/prog3/tpo/dominio/Hub.java
package edu.uade.prog3.tpo.dominio;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.*;

import java.util.Set;

@Node("Hub") // Representa nodos con etiqueta :Hub (Deposito y Sucursal la comparten)
public class Hub {

    @Id
    private String id;

    private String nombre;
    private String barrio;

    // Permite conservar etiquetas adicionales (:Deposito, :Sucursal) que existen en el grafo
    @DynamicLabels
    private Set<String> etiquetas; // ej: ["Deposito"] o ["Sucursal"]

    // Conexiones por :RUTA en ambos sentidos entre hubs
    @Relationship(type = "RUTA", direction = Relationship.Direction.OUTGOING)
    private Set<Hub> rutas;

    public Hub() {}

    public Hub(String id, String nombre, String barrio) {
        this.id = id;
        this.nombre = nombre;
        this.barrio = barrio;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getBarrio() { return barrio; }
    public Set<String> getEtiquetas() { return etiquetas; }
    public Set<Hub> getRutas() { return rutas; }

    public void setId(String id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setBarrio(String barrio) { this.barrio = barrio; }
    public void setEtiquetas(Set<String> etiquetas) { this.etiquetas = etiquetas; }
    public void setRutas(Set<Hub> rutas) { this.rutas = rutas; }
}
