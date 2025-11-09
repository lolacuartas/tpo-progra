package edu.uade.prog3.tpo.dominio;

import java.util.List;

public class ResultadoGreedy{

    private String depositoId;
    private String vehiculoId;
    private String destino; // sucursal o cliente elegido en el endpoint
    private Double pesoTotal;
    private Double volumenTotal;
    private List<Paquete> paquetesAsignados;
    private List<Paquete> paquetesNoAsignados;

    public ResultadoGreedy(String depositoId, String vehiculoId, String destino,
                            Double pesoTotal, Double volumenTotal,
                            List<Paquete> paquetesAsignados,
                            List<Paquete> paquetesNoAsignados) {
        this.depositoId = depositoId;
        this.vehiculoId = vehiculoId;
        this.destino = destino;
        this.pesoTotal = pesoTotal;
        this.volumenTotal = volumenTotal;
        this.paquetesAsignados = paquetesAsignados;
        this.paquetesNoAsignados = paquetesNoAsignados;
    }

    public String getDepositoId() { return depositoId; }
    public String getVehiculoId() { return vehiculoId; }
    public String getDestino() { return destino; }
    public Double getPesoTotal() { return pesoTotal; }
    public Double getVolumenTotal() { return volumenTotal; }
    public List<Paquete> getPaquetesAsignados() { return paquetesAsignados; }
    public List<Paquete> getPaquetesNoAsignados() { return paquetesNoAsignados; }
}
