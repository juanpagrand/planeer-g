package com.planer.planner.controller;

import com.planer.planner.model.PlanMensual;
import com.planer.planner.repository.PlanMensualRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Controller
public class HomeController {

    @Autowired
    private PlanMensualRepository planMensualRepository;

    private String formatearMes(Integer anio, Integer mes) {
        YearMonth ym = YearMonth.of(anio, mes);
        String name = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        return name.substring(0, 1).toUpperCase() + name.substring(1) + " " + anio;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<PlanMensual> activos = planMensualRepository.findByEstado("ACTIVO");
        
        // Pass a formatted title for Thymeleaf
        for (PlanMensual p : activos) {
            model.addAttribute("plan_" + p.getId(), formatearMes(p.getAnio(), p.getMes()));
        }
        
        model.addAttribute("planesActivos", activos);
        return "index";
    }

    @GetMapping("/historial")
    public String historial(Model model) {
        List<PlanMensual> finalizados = planMensualRepository.findByEstado("FINALIZADO");
        
        for (PlanMensual p : finalizados) {
            model.addAttribute("plan_" + p.getId(), formatearMes(p.getAnio(), p.getMes()));
        }
        
        model.addAttribute("planesFinalizados", finalizados);
        return "historial";
    }
}
