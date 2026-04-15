package com.planer.planner.repository;

import com.planer.planner.model.PlanDetalle;
import com.planer.planner.model.PlanMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanDetalleRepository extends JpaRepository<PlanDetalle, Long> {
    List<PlanDetalle> findByPlanMensual(PlanMensual planMensual);
    void deleteByPlanMensual(PlanMensual planMensual);
}
