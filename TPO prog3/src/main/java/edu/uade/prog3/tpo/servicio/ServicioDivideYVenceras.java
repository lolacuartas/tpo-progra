package edu.uade.prog3.tpo.servicio;

import edu.uade.prog3.tpo.repositorio.IRepositorioGrafo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ServicioDivideYVenceras {

    private final IRepositorioGrafo repo;

    public ServicioDivideYVenceras(IRepositorioGrafo repo) {
        this.repo = repo;
    }

    /**
     * Trae los paquetes pendientes de un depósito y los ordena
     * usando MergeSort (divide y vencerás) por:
     *  1) prioridad ASC
     *  2) peso_kg ASC
     */
    public List<Map<String, Object>> ordenarPaquetesPorDyV(String depositoId) {

        // 1) traer datos de Neo4j
        List<Map<String, Object>> paquetesFromDb = repo.paquetesPendientesDeDeposito(depositoId);

        // los copio porque la lista puede ser inmodificable
        List<Map<String, Object>> paquetes = new ArrayList<>(paquetesFromDb);

        // 2) aplicar merge sort
        return mergeSort(paquetes);
    }

    // =========================
    //  MERGE SORT (Divide y vencerás)
    // =========================

    private List<Map<String, Object>> mergeSort(List<Map<String, Object>> lista) {
        if (lista.size() <= 1) {
            return lista;
        }

        int mid = lista.size() / 2;
        List<Map<String, Object>> izquierda = mergeSort(new ArrayList<>(lista.subList(0, mid)));
        List<Map<String, Object>> derecha = mergeSort(new ArrayList<>(lista.subList(mid, lista.size())));

        return merge(izquierda, derecha);
    }

    private List<Map<String, Object>> merge(List<Map<String, Object>> izq, List<Map<String, Object>> der) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        int i = 0, j = 0;

        while (i < izq.size() && j < der.size()) {
            Map<String, Object> pIzq = izq.get(i);
            Map<String, Object> pDer = der.get(j);

            if (esMenorOIgual(pIzq, pDer)) {
                resultado.add(pIzq);
                i++;
            } else {
                resultado.add(pDer);
                j++;
            }
        }

        // agregar lo que quedó
        while (i < izq.size()) {
            resultado.add(izq.get(i));
            i++;
        }
        while (j < der.size()) {
            resultado.add(der.get(j));
            j++;
        }

        return resultado;
    }

    /**
     * Compara dos paquetes (Map) según nuestra regla:
     * primero prioridad ASC, después peso_kg ASC.
     */
    private boolean esMenorOIgual(Map<String, Object> a, Map<String, Object> b) {
        int prioridadA = a.get("prioridad") == null ? 999 : ((Number) a.get("prioridad")).intValue();
        int prioridadB = b.get("prioridad") == null ? 999 : ((Number) b.get("prioridad")).intValue();

        if (prioridadA != prioridadB) {
            return prioridadA < prioridadB;
        }

        double pesoA = a.get("peso_kg") == null ? Double.MAX_VALUE : ((Number) a.get("peso_kg")).doubleValue();
        double pesoB = b.get("peso_kg") == null ? Double.MAX_VALUE : ((Number) b.get("peso_kg")).doubleValue();

        return pesoA <= pesoB;
    }
}
