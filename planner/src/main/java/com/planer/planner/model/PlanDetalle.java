package com.planer.planner.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
public class PlanDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_mensual_id", nullable = false)
    private PlanMensual planMensual;

    @ManyToOne
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    private Integer dia;

    private String estado = "EJECUTADO";

    public PlanDetalle() {}

    public PlanDetalle(PlanMensual planMensual, Equipo equipo, Integer dia) {
        this.planMensual = planMensual;
        this.equipo = equipo;
        this.dia = dia;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PlanMensual getPlanMensual() { return planMensual; }
    public void setPlanMensual(PlanMensual planMensual) { this.planMensual = planMensual; }
    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }
    public Integer getDia() { return dia; }
    public void setDia(Integer dia) { this.dia = dia; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
