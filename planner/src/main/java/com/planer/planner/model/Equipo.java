package com.planer.planner.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "equipos")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tag;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(name = "criterio_programacion")
    private String criterioProgramacion;

    private String taller;

    private String tipo;

    private Double buque;


    @Column(name = "mes_inicial")
    private String mesInicial; // e.g., "Enero", "Febrero", etc.

    @Column(name = "fecha_proxima")
    private LocalDate fechaProxima;

    private String duracion;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer criticidad = 0;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean reprogramado = false;

    public Equipo() {}

    public Equipo(String tag, String descripcion, String criterioProgramacion, String taller, String mesInicial, LocalDate fechaProxima, String duracion, String tipo, Double buque) {
        this.tag = tag;
        this.descripcion = descripcion;
        this.criterioProgramacion = criterioProgramacion;
        this.taller = taller;
        this.mesInicial = mesInicial;
        this.fechaProxima = fechaProxima;
        this.duracion = duracion;
        this.tipo = tipo;
        this.buque = buque;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCriterioProgramacion() { return criterioProgramacion; }
    public void setCriterioProgramacion(String criterioProgramacion) { this.criterioProgramacion = criterioProgramacion; }

    public String getTaller() { return taller; }
    public void setTaller(String taller) { this.taller = taller; }

    @Transient
    public String getTallerNombre() {
        if (taller == null) return "N/A";
        switch (taller.toUpperCase().trim()) {
            case "M": return "Mecánico";
            case "I": return "Instrumentista";
            case "E": return "Electricista";
            case "C": return "Civil";
            case "P": return "Pintor";
            default: return taller;
        }
    }

    public String getMesInicial() { return mesInicial; }
    public void setMesInicial(String mesInicial) { this.mesInicial = mesInicial; }

    public LocalDate getFechaProxima() { return fechaProxima; }
    public void setFechaProxima(LocalDate fechaProxima) { this.fechaProxima = fechaProxima; }

    public String getDuracion() { return duracion; }
    public void setDuracion(String duracion) { this.duracion = duracion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Integer getCriticidad() { return criticidad; }
    public void setCriticidad(Integer criticidad) { this.criticidad = criticidad; }

    public Boolean getReprogramado() { return reprogramado; }
    public void setReprogramado(Boolean reprogramado) { this.reprogramado = reprogramado; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Double getBuque() { return buque; }
    public void setBuque(Double buque) { this.buque = buque; }
}
