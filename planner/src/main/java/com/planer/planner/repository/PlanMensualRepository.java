package com.planer.planner.repository;

import com.planer.planner.model.PlanMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanMensualRepository extends JpaRepository<PlanMensual, Long> {
    
    List<PlanMensual> findByAnioAndMes(Integer anio, Integer mes);
    
    List<PlanMensual> findByEstado(String estado);
}
