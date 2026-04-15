package com.planer.planner.controller;

import com.planer.planner.model.Equipo;
import com.planer.planner.repository.EquipoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class EquipoController {

    @Autowired
    private EquipoRepository equipoRepository;

    @GetMapping("/equipos")
    public String listarEquipos(Model model) {
        List<Equipo> equipos = equipoRepository.findByTipoNotIgnoreCaseOrTipoIsNull("correctivo");
        model.addAttribute("equipos", equipos);
        return "equipos";
    }

    @GetMapping("/correctivos")
    public String listarCorrectivos(Model model) {
        List<Equipo> correctivos = equipoRepository.findByTipoIgnoreCase("correctivo");
        model.addAttribute("equipos", correctivos);
        return "correctivos";
    }

    @PostMapping("/equipos/guardar")
    public String guardarEquipo(@ModelAttribute Equipo equipo) {
        if (equipo.getActivo() == null) {
            equipo.setActivo(true);
        }
        if (equipo.getTipo() == null || equipo.getTipo().isEmpty()) {
            equipo.setTipo("preventivo");
        }
        equipoRepository.save(equipo);
        if ("correctivo".equalsIgnoreCase(equipo.getTipo())) {
            return "redirect:/correctivos";
        }
        return "redirect:/equipos";
    }

    @PostMapping("/equipos/toggle/{id}")
    public String toggleEstado(@PathVariable Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        String redirectUrl = "redirect:/equipos";
        if (equipo != null) {
            // Check if getActivo is null to prevent null pointer exceptions
            boolean currentStatus = (equipo.getActivo() != null) ? equipo.getActivo() : true;
            equipo.setActivo(!currentStatus);
            equipoRepository.save(equipo);
            if ("correctivo".equalsIgnoreCase(equipo.getTipo())) {
                redirectUrl = "redirect:/correctivos";
            }
        }
        return redirectUrl;
    }
}
