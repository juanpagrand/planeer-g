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
                .filter(d -> !"REPROGRAMADO".equals(d.getEstado()) && !"NO_EJECUTADO".equals(d.getEstado()))
                .filter(d -> {
                    String tipo = d.getEquipo().getTipo();
                    return tipo == null || !tipo.equalsIgnoreCase("correctivo");
                })
                .count();

        long correctivosRealizados = detalles.stream()
                .filter(d -> !"REPROGRAMADO".equals(d.getEstado()) && !"NO_EJECUTADO".equals(d.getEstado()))
                .filter(d -> {
                    String tipo = d.getEquipo().getTipo();
                    return tipo != null && tipo.equalsIgnoreCase("correctivo");
                })
                .count();

        long noEjecutadosCriticos = detalles.stream()
                .filter(d -> "REPROGRAMADO".equals(d.getEstado()) || "NO_EJECUTADO".equals(d.getEstado()))
                .filter(d -> d.getEquipo().getCriticidad() != null && d.getEquipo().getCriticidad() > 0)
                .count();

        long ejecutadosCriticos = detalles.stream()
                .filter(d -> !"REPROGRAMADO".equals(d.getEstado()) && !"NO_EJECUTADO".equals(d.getEstado()))
                .filter(d -> d.getEquipo().getCriticidad() != null && d.getEquipo().getCriticidad() > 0)
                .count();

        long noEjecutadosNormales = detalles.stream()
                .filter(d -> "REPROGRAMADO".equals(d.getEstado()) || "NO_EJECUTADO".equals(d.getEstado()))
                .filter(d -> d.getEquipo().getCriticidad() == null || d.getEquipo().getCriticidad() == 0)
                .count();

        // Total excludes NO_EJECUTADO and REPROGRAMADO
        long totalRealizados = detalles.stream().filter(d -> !"REPROGRAMADO".equals(d.getEstado()) && !"NO_EJECUTADO".equals(d.getEstado())).count();

        // Para equipos mantenidos (ejecutados + no ejecutados) podemos pasarlos todos y la vista los diferenciará
        List<PlanDetalle> detallesKpi = detalles.stream()
            // Filtramos para obtener un detalle por equipo (distinct by equipoId)
            .collect(Collectors.toMap(
                d -> d.getEquipo().getId(), 
                d -> d, 
                (existing, replacement) -> existing // si hay colisión, mantiene el primero
            ))
            .values()
            .stream()
            .collect(Collectors.toList());

        model.addAttribute("mesFormateado", formatearMes(plan.getAnio(), plan.getMes()));
        model.addAttribute("plan", plan);
        model.addAttribute("totalRealizados", totalRealizados);
        model.addAttribute("preventivosRealizados", preventivosRealizados);
        model.addAttribute("correctivosRealizados", correctivosRealizados);
        model.addAttribute("ejecutadosCriticos", ejecutadosCriticos);
        model.addAttribute("noEjecutadosCriticos", noEjecutadosCriticos);
        model.addAttribute("noEjecutadosNormales", noEjecutadosNormales);
        model.addAttribute("detallesKpi", detallesKpi);

        return "kpi-detalle";
    }
}
