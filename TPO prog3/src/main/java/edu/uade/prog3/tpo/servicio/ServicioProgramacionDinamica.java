package edu.uade.prog3.tpo.servicio;

import edu.uade.prog3.tpo.repositorio.IRepositorioGrafo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServicioProgramacionDinamica {

    private final IRepositorioGrafo repo;

    public ServicioProgramacionDinamica(IRepositorioGrafo repo) {
        this.repo = repo;
    }

    /**
     * Mochila 0/1 con Programación Dinámica.
     * - Usa SOLO el peso (peso_kg) como restricción.
     * - Usa la prioridad para armar un "valor" (prioridad 1 = más valor).
     * - Actualiza en Neo4j los paquetes elegidos.
     */
    public Map<String, Object> ejecutarMochilaDp(String depositoId,
                                                 String vehiculoId,
                                                 String destino) {

        // 1) Traer paquetes pendientes
        List<Map<String, Object>> paquetesDb = repo.paquetesPendientesDeDeposito(depositoId);
        List<Map<String, Object>> paquetes = new ArrayList<>(paquetesDb);

        // 2) Traer vehículos
        List<Map<String, Object>> vehiculosDb = repo.vehiculosDeDeposito(depositoId);
        List<Map<String, Object>> vehiculos = new ArrayList<>(vehiculosDb);
        if (vehiculos.isEmpty()) {
            throw new RuntimeException("El depósito " + depositoId + " no tiene vehículos.");
        }

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
            vehiculoSel = vehiculos.get(0);
        }

        // capacidad en kg (lo pasamos a int)
        int capacidad = ((Number) vehiculoSel.get("capacidad_kg")).intValue();

        int n = paquetes.size();
        if (n == 0) {
            Map<String, Object> respVacia = new LinkedHashMap<>();
            respVacia.put("depositoId", depositoId);
            respVacia.put("vehiculoId", vehiculoSel.get("id"));
            respVacia.put("destino", destino);
            respVacia.put("pesoTotal", 0);
            respVacia.put("valorTotal", 0);
            respVacia.put("paquetesSeleccionados", List.of());
            respVacia.put("paquetesNoSeleccionados", List.of());
            return respVacia;
        }

        // =========================
        //  ARMAR TABLA DP
        // =========================
        // dp[i][w] = mejor valor usando los primeros i paquetes con capacidad w
        int[][] dp = new int[n + 1][capacidad + 1];

        // Para reconstruir después qué paquetes se eligieron
        // recorro paquetes 1..n
        for (int i = 1; i <= n; i++) {
            Map<String, Object> p = paquetes.get(i - 1);

            // peso del paquete (int)
            int peso = ((Number) p.get("peso_kg")).intValue();

            // valor del paquete: cuanto más baja la prioridad, más vale
            // prioridad 1 -> valor 3000
            // prioridad 2 -> valor 2000
            // prioridad 3 -> valor 1000
            int prioridad = p.get("prioridad") == null ? 3 : ((Number) p.get("prioridad")).intValue();
            int valor = (4 - prioridad) * 1000;

            for (int w = 0; w <= capacidad; w++) {
                // si no entra
                if (peso > w) {
                    dp[i][w] = dp[i - 1][w];
                } else {
                    // elijo lo mejor entre NO usarlo y usarlo
                    dp[i][w] = Math.max(
                            dp[i - 1][w],                      // no usar paquete i
                            valor + dp[i - 1][w - peso]        // usar paquete i
                    );
                }
            }
        }

        // =========================
        //  RECONSTRUIR SOLUCIÓN
        // =========================
        int w = capacidad;
        List<Map<String, Object>> seleccionados = new ArrayList<>();
        List<Map<String, Object>> noSeleccionados = new ArrayList<>();

        for (int i = n; i >= 1; i--) {
            Map<String, Object> p = paquetes.get(i - 1);
            int peso = ((Number) p.get("peso_kg")).intValue();
            int prioridad = p.get("prioridad") == null ? 3 : ((Number) p.get("prioridad")).intValue();
            int valor = (4 - prioridad) * 1000;

            // si el valor viene del item i, entonces lo usamos
            if (w >= peso && dp[i][w] == valor + dp[i - 1][w - peso]) {
                seleccionados.add(p);
                w = w - peso;

                String paqueteId = (String) p.get("id");
                // persistimos
                repo.actualizarEstadoPaquete(paqueteId, "asignado_dp");
                if (destino != null && !destino.isBlank()) {
                    repo.setearDestinoPaquete(paqueteId, destino);
                }
                repo.crearRelacionTransporte((String) vehiculoSel.get("id"), paqueteId, destino);
            } else {
                noSeleccionados.add(p);
            }
        }

        // como fuimos de atrás para adelante, la lista quedó invertida
        Collections.reverse(seleccionados);

        // calcular peso total
        int pesoTotal = seleccionados.stream()
                .mapToInt(p -> ((Number) p.get("peso_kg")).intValue())
                .sum();

        // armar respuesta
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("depositoId", depositoId);
        resp.put("vehiculoId", vehiculoSel.get("id"));
        resp.put("destino", destino);
        resp.put("pesoTotal", pesoTotal);
        resp.put("valorTotal", dp[n][capacidad]);
        resp.put("paquetesSeleccionados", seleccionados);
        resp.put("paquetesNoSeleccionados", noSeleccionados);

        return resp;
    }
}
