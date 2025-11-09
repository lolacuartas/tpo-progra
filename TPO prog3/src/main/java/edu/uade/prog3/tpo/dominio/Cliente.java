// src/main/java/edu/uade/prog3/tpo/dominio/Cliente.java
package edu.uade.prog3.tpo.dominio;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.*;

@Node("Cliente")
public class Cliente {

    @Id
    private String id;

    private String nombre;

    // La relación en tu dataset es Sucursal(:Hub) -[:ATIENDE]-> Cliente
    @Relationship(type = "ATIENDE", direction = Relationship.Direction.INCOMING)
    private Hub sucursal; // quién lo atiende

    public Cliente() {}

    public Cliente(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public Hub getSucursal() { return sucursal; }

    public void setId(String id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setSucursal(Hub sucursal) { this.sucursal = sucursal; }
}
