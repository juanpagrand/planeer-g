package com.planer.planner.repository;

import com.planer.planner.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    List<Equipo> findByTipoIgnoreCase(String tipo);
    List<Equipo> findByTipoNotIgnoreCaseOrTipoIsNull(String tipo);
}
