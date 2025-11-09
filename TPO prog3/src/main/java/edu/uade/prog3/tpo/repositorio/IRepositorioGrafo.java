// src/main/java/edu/uade/prog3/tpo/repositorio/IRepositorioGrafo.java
package edu.uade.prog3.tpo.repositorio;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class IRepositorioGrafo {

    private final Neo4jClient neo4j;

    public IRepositorioGrafo(Neo4jClient neo4j) {
        this.neo4j = neo4j;
    }

    public boolean existeHubPorId(String id) {
        String q = """
            MATCH (h:Hub {id:$id}) RETURN count(h) AS c
        """;
        return neo4j.query(q).bind(id).to("id").fetchAs(Long.class).one().orElse(0L) > 0;
    }

    public boolean existeClientePorId(String id) {
        String q = """
            MATCH (c:Cliente {id:$id}) RETURN count(c) AS c
        """;
        return neo4j.query(q).bind(id).to("id").fetchAs(Long.class).one().orElse(0L) > 0;
    }

    /** Vecinos solo por rutas entre Hubs (:RUTA, sin dirección). */
    public List<String> vecinosHubs(String hubId) {
        String q = """
            MATCH (n:Hub {id:$id})-[:RUTA]-(m:Hub)
            RETURN DISTINCT m.id AS vecino
            ORDER BY vecino
        """;
        return neo4j.query(q).bind(hubId).to("id").fetch().all().stream()
                .map(r -> (String) r.get("vecino")).toList();
    }

    /** Vecinos incluyendo Clientes (hop por :ATIENDE de/desde una Sucursal). */
    public List<String> vecinosIncluyendoClientes(String nodoId) {
        String q = """
        CALL {
          WITH $id AS id
          MATCH (n {id:id})-[:RUTA]-(h:Hub)
          RETURN h.id AS vecino
          UNION
          WITH $id AS id
          MATCH (n {id:id})-[:ATIENDE]-(c:Cliente)
          RETURN c.id AS vecino
        }
        RETURN DISTINCT vecino
        ORDER BY vecino
    """;
        return neo4j.query(q)
                .bind(nodoId).to("id")
                .fetch().all().stream()
                .map(r -> (String) r.get("vecino"))
                .toList();
    }

    /** Nombre legible (sirve para Deposito/Sucursal/Cliente indistintamente). */
    public Optional<String> nombreDe(String id) {
        String q = """
            MATCH (n {id:$id}) RETURN n.nombre AS nombre LIMIT 1
        """;
        return neo4j.query(q).bind(id).to("id").fetch().one().map(m -> (String) m.get("nombre"));
    }


    /** Utilidad para nombres masivos. */
    public List<String> nombresDe(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String q = """
            UNWIND $ids AS i
            MATCH (n {id:i})
            RETURN i AS id, n.nombre AS nombre
        """;
        Map<String, String> nombres = neo4j.query(q)
                .bind(ids).to("ids")
                .fetch().all().stream()
                .collect(Collectors.toMap(
                        r -> (String) r.get("id"),
                        r -> (String) r.getOrDefault("nombre", r.get("id")),
                        //si viene la misma key dos veces, uso la primera para no romper el map
                        (n1, n2) -> n1

                ));
        return ids.stream().map(i -> nombres.getOrDefault(i, i)).toList();
    }



    /** Dado un cliente, devuelve la sucursal que lo atiende (para atajo a nivel de negocio). */
    public Optional<String> sucursalQueAtiendeCliente(String clienteId) {
        String q = """
            MATCH (s:Sucursal)-[:ATIENDE]->(c:Cliente {id:$id})
            RETURN s.id AS suc LIMIT 1
        """;
        return neo4j.query(q).bind(clienteId).to("id").fetch().one().map(m -> (String) m.get("suc"));
    }

    /** Dado un cliente, devuelve todas las sucursales que lo atienden. */
    public List<String> sucursalesQueAtiendenCliente(String clienteId) {
        String q = """
        MATCH (s:Sucursal:Hub)-[:ATIENDE]->(c:Cliente {id:$id})
        RETURN s.id AS suc
    """;
        return neo4j.query(q)
                .bind(clienteId).to("id")
                .fetch().all().stream()
                .map(r -> (String) r.get("suc"))
                .toList();
    }




    /** Devuelve vecinos Hub -> Hub, con pesos de la relación :RUTA (solo OUTGOING). */
    // java
    public List<Map<String, Object>> vecinosConPesos(String hubId) {
        String q = """
        MATCH (n:Hub {id:$id})-[r:RUTA]->(m:Hub)
        RETURN m.id AS vecino, r.dist_km AS dist, r.tiempo_min AS tiempo, r.peaje AS peaje
        ORDER BY vecino
    """;
        return neo4j.query(q).bind(hubId).to("id").fetch().all().stream().toList();
    }

    public List<String> todosLosHubs() {
        String q = "MATCH (h:Hub) RETURN h.id AS id ORDER BY id";
        return neo4j.query(q).fetch().all().stream()
                .map(r -> (String) r.get("id")).toList();
    }

    // =========================
    //  PAQUETES / VEHÍCULOS
    // =========================

    /**
     * Devuelve los paquetes pendientes originados en un depósito.
     * MATCH (d:Deposito {id:$depositoId})-[:ORIGINA]->(p:Paquete {estado:'pendiente'})
     */
    public List<Map<String, Object>> paquetesPendientesDeDeposito(String depositoId) {
        String q = """
                MATCH (d:Deposito:Hub {id:$dep})-[:ORIGINA]->(p:Paquete)
                WHERE p.estado = 'pendiente'
                RETURN p.id AS id,
                       p.peso_kg AS peso_kg,
                       p.volumen_m3 AS volumen_m3,
                       p.prioridad AS prioridad,
                       p.estado AS estado
                ORDER BY prioridad ASC, peso_kg ASC
                """;
        return neo4j.query(q)
                .bind(depositoId).to("dep")
                .fetch().all().stream().toList();
    }

    /**
     * Devuelve los vehículos que tiene un depósito.
     * MATCH (d:Deposito {id:$depositoId})-[:TIENE_VEHICULO]->(v:Vehiculo)
     */
    public List<Map<String, Object>> vehiculosDeDeposito(String depositoId) {
        String q = """
                MATCH (d:Deposito:Hub {id:$dep})-[:TIENE_VEHICULO]->(v:Vehiculo)
                RETURN v.id AS id,
                       v.patente AS patente,
                       v.capacidad_kg AS capacidad_kg,
                       v.volumen_m3 AS volumen_m3,
                       v.costo_km AS costo_km,
                       v.tipo AS tipo
                ORDER BY id
                """;
        return neo4j.query(q)
                .bind(depositoId).to("dep")
                .fetch().all().stream().toList();
    }

    /**
     * Actualiza el estado de un paquete.
     */
    public void actualizarEstadoPaquete(String paqueteId, String nuevoEstado) {
        String q = """
                MATCH (p:Paquete {id:$id})
                SET p.estado = $estado
                """;
        neo4j.query(q)
                .bind(paqueteId).to("id")
                .bind(nuevoEstado).to("estado")
                .run();
    }

    /**
     * Setea el destino en el paquete (porque vos lo pasás por endpoint).
     */
    public void setearDestinoPaquete(String paqueteId, String destino) {
        String q = """
                MATCH (p:Paquete {id:$id})
                SET p.destino = $destino
                """;
        neo4j.query(q)
                .bind(paqueteId).to("id")
                .bind(destino).to("destino")
                .run();
    }

    /**
     * (Opcional) Crear relación de viaje: (v)-[:TRANSPORTA {destino:..., fecha:datetime()}]->(p)
     */
    public void crearRelacionTransporte(String vehiculoId, String paqueteId, String destino) {
        String q = """
                MATCH (v:Vehiculo {id:$veh}), (p:Paquete {id:$paq})
                MERGE (v)-[t:TRANSPORTA]->(p)
                SET t.destino = $destino,
                    t.fecha = datetime()
                """;
        neo4j.query(q)
                .bind(vehiculoId).to("veh")
                .bind(paqueteId).to("paq")
                .bind(destino).to("destino")
                .run();
    }

}
