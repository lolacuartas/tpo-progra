package edu.uade.prog3.tpo.servicio;

import edu.uade.prog3.tpo.repositorio.IRepositorioGrafo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServicioGreedy {

    private final IRepositorioGrafo repo;

    public ServicioGreedy(IRepositorioGrafo repo) {
        this.repo = repo;
    }

    public Map<String, Object> ejecutarGreedy(String depositoId, String vehiculoId, String destino) {

        // 1) paquetes pendientes (puede venir como lista inmodificable → la copio)
        List<Map<String, Object>> paquetesFromDb = repo.paquetesPendientesDeDeposito(depositoId);
        List<Map<String, Object>> paquetes = new ArrayList<>(paquetesFromDb);   // <-- FIX

        // 2) vehículos del depósito
        List<Map<String, Object>> vehiculosFromDb = repo.vehiculosDeDeposito(depositoId);
        List<Map<String, Object>> vehiculos = new ArrayList<>(vehiculosFromDb); // por las dudas

        if (vehiculos.isEmpty()) {
            throw new RuntimeException("El depósito " + depositoId + " no tiene vehículos.");
        }

        // 3) elegir vehículo
        Map<String, Object> vehiculoSel = null;
        if (vehiculoId != null && !vehiculoId.isBlank()) {
            for (Map<String, Object> v : vehiculos) {
                if (vehiculoId.equals(v.get("id"))) {
                    vehiculoSel = v;
                    break;
                }
            }
            if (vehiculoSel == null) {
                throw new RuntimeException("El vehículo " + vehiculoId + " no pertenece al depósito " + depositoId);
            }
        } else {
            // si no mandan, tomo el primero
            vehiculoSel = vehiculos.get(0);
        }

        double capPeso = ((Number) vehiculoSel.get("capacidad_kg")).doubleValue();
        double capVol = ((Number) vehiculoSel.get("volumen_m3")).doubleValue();

        // 4) ORDEN HEURÍSTICO
        // prioridad ASC (1 primero), y dentro de misma prioridad, peso ASC
        paquetes.sort(Comparator
                .comparing((Map<String, Object> p) -> {
                    Object pr = p.get("prioridad");
                    return pr == null ? 999 : ((Number) pr).intValue();
                })
                .thenComparing(p -> {
                    Object pes = p.get("peso_kg");
                    return pes == null ? Double.MAX_VALUE : ((Number) pes).doubleValue();
                })
        );

        List<Map<String, Object>> asignados = new ArrayList<>();
        List<Map<String, Object>> noAsignados = new ArrayList<>();

        double pesoUsado = 0.0;
        double volUsado = 0.0;

        for (Map<String, Object> p : paquetes) {
            double peso = ((Number) p.get("peso_kg")).doubleValue();
            double vol = ((Number) p.get("volumen_m3")).doubleValue();

            if (pesoUsado + peso <= capPeso && volUsado + vol <= capVol) {
                // entra
                asignados.add(p);
                pesoUsado += peso;
                volUsado += vol;

                String paqueteId = (String) p.get("id");

                // actualizar en Neo4j
                repo.actualizarEstadoPaquete(paqueteId, "asignado");
                if (destino != null && !destino.isBlank()) {
                    repo.setearDestinoPaquete(paqueteId, destino);
                }
                // opcional: relación
                repo.crearRelacionTransporte((String) vehiculoSel.get("id"), paqueteId, destino);

            } else {
                noAsignados.add(p);
            }
        }

        // armo respuesta
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("depositoId", depositoId);
        resp.put("vehiculoId", vehiculoSel.get("id"));
        resp.put("destino", destino);
        resp.put("pesoTotal", pesoUsado);
        resp.put("volumenTotal", volUsado);
        resp.put("paquetesAsignados", asignados);
        resp.put("paquetesNoAsignados", noAsignados);

        return resp;
    }
}
