package com.planer.planner.controller;

import com.planer.planner.model.Equipo;
import com.planer.planner.model.PlanDetalle;
import com.planer.planner.model.PlanMensual;
import com.planer.planner.repository.PlanDetalleRepository;
import com.planer.planner.repository.PlanMensualRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class KpiController {

    @Autowired
    private PlanMensualRepository planMensualRepository;

    @Autowired
    private PlanDetalleRepository planDetalleRepository;

    private String formatearMes(Integer anio, Integer mes) {
        YearMonth ym = YearMonth.of(anio, mes);
        String name = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        return name.substring(0, 1).toUpperCase() + name.substring(1) + " " + anio;
    }

    @GetMapping("/kpi")
    public String listarKpis(Model model) {
        List<PlanMensual> finalizados = planMensualRepository.findByEstado("FINALIZADO");
        
        for (PlanMensual p : finalizados) {
            model.addAttribute("plan_" + p.getId(), formatearMes(p.getAnio(), p.getMes()));
        }
        
        model.addAttribute("planesFinalizados", finalizados);
        return "kpi";
    }

    @GetMapping("/kpi/detalle")
    public String detalleKpi(@RequestParam("id") Long id, Model model) {
        Optional<PlanMensual> planOpt = planMensualRepository.findById(id);
        if (!planOpt.isPresent()) {
            return "redirect:/kpi";
        }
        
        PlanMensual plan = planOpt.get();
        List<PlanDetalle> detalles = planDetalleRepository.findByPlanMensual(plan);
        
        long preventivosRealizados = detalles.stream()
                .filter(d -> {
                    String tipo = d.getEquipo().getTipo();
                    return tipo == null || !tipo.equalsIgnoreCase("correctivo");
                })
                .count();

        long correctivosRealizados = detalles.stream()
                .filter(d -> {
                    String tipo = d.getEquipo().getTipo();
                    return tipo != null && tipo.equalsIgnoreCase("correctivo");
                })
                .count();

        List<Equipo> equiposCriticos = detalles.stream()
            .map(PlanDetalle::getEquipo)
            .filter(e -> e.getCriticidad() != null && e.getCriticidad() > 0)
            // Para eliminar posibles duplicados si algun equipo estuvo varias veces en el mes
            .distinct()
            .collect(Collectors.toList());

        model.addAttribute("mesFormateado", formatearMes(plan.getAnio(), plan.getMes()));
        model.addAttribute("plan", plan);
        model.addAttribute("totalRealizados", detalles.size());
        model.addAttribute("preventivosRealizados", preventivosRealizados);
        model.addAttribute("correctivosRealizados", correctivosRealizados);
        model.addAttribute("equiposCriticos", equiposCriticos);

        return "kpi-detalle";
    }
}
