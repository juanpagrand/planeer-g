package com.planer.planner.controller;

import com.planer.planner.model.Equipo;
import com.planer.planner.model.PlanDetalle;
import com.planer.planner.model.PlanMensual;
import com.planer.planner.repository.EquipoRepository;
import com.planer.planner.repository.PlanDetalleRepository;
import com.planer.planner.repository.PlanMensualRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class ProgramacionController {

    @Autowired
    private PlanMensualRepository planMensualRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private PlanDetalleRepository planDetalleRepository;

    @GetMapping("/calendario")
    public String verCalendario(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "taller", required = false) String taller,
            Model model) {

        int y, m;
        String estadoActual = "NUEVO";
        Long planId = 0L;
        List<PlanDetalle> detalles = new ArrayList<>();

        if (id != null && id > 0) {
            Optional<PlanMensual> planOpt = planMensualRepository.findById(id);
            if (planOpt.isPresent()) {
                y = planOpt.get().getAnio();
                m = planOpt.get().getMes();
                estadoActual = planOpt.get().getEstado();
                planId = id;
                detalles = planDetalleRepository.findByPlanMensual(planOpt.get());
            } else {
                return "redirect:/"; // ID no encontrado
            }
        } else {
            LocalDate hoy = LocalDate.now();
            y = (year != null) ? year : hoy.getYear();
            m = (month != null) ? month : hoy.getMonthValue();
            if (m < 1 || m > 12) {
                m = hoy.getMonthValue();
            }

            // Verificar si YA existe un plan ACTIVO para este mes
            List<PlanMensual> existentes = planMensualRepository.findByAnioAndMes(y, m);
            boolean yaEstaActivo = existentes.stream().anyMatch(p -> "ACTIVO".equals(p.getEstado()));

            if (yaEstaActivo) {
                return "redirect:/?error=existe_activo";
            }
        }

        YearMonth yearMonth = YearMonth.of(y, m);

        String mesNombre = yearMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        mesNombre = mesNombre.substring(0, 1).toUpperCase() + mesNombre.substring(1);

        int totalDias = yearMonth.lengthOfMonth();
        int diaSemanaInicio = yearMonth.atDay(1).getDayOfWeek().getValue();

        List<Integer> diasPrevios = new ArrayList<>();
        for (int i = 1; i < diaSemanaInicio; i++) {
            diasPrevios.add(i);
        }

        List<Integer> diasDelMes = new ArrayList<>();
        for (int i = 1; i <= totalDias; i++) {
            diasDelMes.add(i);
        }

        model.addAttribute("mesNombre", mesNombre);
        model.addAttribute("anio", y);
        model.addAttribute("mes", m);
        model.addAttribute("diasPrevios", diasPrevios);
        model.addAttribute("diasDelMes", diasDelMes);

        model.addAttribute("planId", planId);
        model.addAttribute("estado", estadoActual);

        if (taller == null)
            taller = "Todos";
        model.addAttribute("tallerNavegacion", taller);

        final int filterYear = y;
        final int filterMonth = m;
        final String expectedTaller = taller;
        final boolean isFinalizado = "FINALIZADO".equals(estadoActual);
        final List<Long> equiposGuardadosIds = detalles.stream().map(d -> d.getEquipo().getId()).toList();

        List<Equipo> equipos = equipoRepository.findAll().stream()
                .filter(e -> {
                    if (equiposGuardadosIds.contains(e.getId())) return true;
                    return Boolean.TRUE.equals(e.getActivo());
                })
                .filter(e -> {
                    if (equiposGuardadosIds.contains(e.getId()))
                        return true;
                    if (isFinalizado)
                        return false;

                    if (e.getFechaProxima() != null) {
                        return e.getFechaProxima().getYear() == filterYear &&
                                e.getFechaProxima().getMonthValue() == filterMonth;
                    }
                    return false;
                })
                .filter(e -> {
                    if ("Todos".equals(expectedTaller))
                        return true;
                    if (e.getTaller() == null)
                        return false;
                    String t = e.getTaller();
                    return t.equalsIgnoreCase(expectedTaller) ||
                            t.toUpperCase().equals(expectedTaller.substring(0, 1).toUpperCase());
                })
                .sorted((e1, e2) -> {
                    int c1 = e1.getCriticidad() != null ? e1.getCriticidad() : 0;
                    int c2 = e2.getCriticidad() != null ? e2.getCriticidad() : 0;
                    return Integer.compare(c2, c1);
                })
                .toList();
        model.addAttribute("equipos", equipos);
        model.addAttribute("detallesGuardados", detalles);

        return "calendario";
    }

    @PostMapping("/calendario/guardar")
    public String guardarPlan(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam(value = "asignacionesJson", required = false) String asignacionesJson) {

        PlanMensual plan;
        if (id != null && id > 0) {
            Optional<PlanMensual> planOpt = planMensualRepository.findById(id);
            if (!planOpt.isPresent())
                return "redirect:/";
            plan = planOpt.get();
        } else {
            List<PlanMensual> existentes = planMensualRepository.findByAnioAndMes(year, month);
            boolean yaEstaActivo = existentes.stream().anyMatch(p -> "ACTIVO".equals(p.getEstado()));
            if (yaEstaActivo) {
                return "redirect:/?error=existe_activo";
            }
            plan = new PlanMensual(year, month, "ACTIVO");
            plan = planMensualRepository.save(plan);
        }

        if (asignacionesJson != null && !asignacionesJson.isEmpty()) {
            try {
                String stripped = asignacionesJson.replaceAll("\\s", "");
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"equipoId\":(\\d+),\"dia\":(\\d+)");
                java.util.regex.Matcher m = p.matcher(stripped);

                List<PlanDetalle> prevs = planDetalleRepository.findByPlanMensual(plan);
                // Solo borrar los previos que hayan sido EJECUTADOS. Respetar los REPROGRAMADOS
                List<PlanDetalle> paraBorrar = prevs.stream()
                    .filter(pd -> "EJECUTADO".equals(pd.getEstado()) || pd.getEstado() == null)
                    .toList();
                planDetalleRepository.deleteAll(paraBorrar);

                while (m.find()) {
                    Long eqId = Long.parseLong(m.group(1));
                    Integer dia = Integer.parseInt(m.group(2));
                    Optional<Equipo> eqOpt = equipoRepository.findById(eqId);
                    if (eqOpt.isPresent()) {
                        Equipo eq = eqOpt.get();
                        boolean valid = false;

                        if (prevs.stream().anyMatch(pd -> pd.getEquipo().getId().equals(eq.getId()))) {
                            valid = true;
                        } else if (eq.getFechaProxima() != null &&
                                eq.getFechaProxima().getYear() == year &&
                                eq.getFechaProxima().getMonthValue() == month) {
                            valid = true;
                        }

                        if (valid) {
                            // Por seguridad, si de alguna forma se reprogramó antes pero ahora sí se arrastra:
                            List<PlanDetalle> huerfanos = planDetalleRepository.findByPlanMensual(plan).stream()
                                    .filter(d -> d.getEquipo().getId().equals(eq.getId()))
                                    .toList();
                            planDetalleRepository.deleteAll(huerfanos);

                            PlanDetalle det = new PlanDetalle(plan, eq, dia);
                            det.setEstado("EJECUTADO");
                            planDetalleRepository.save(det);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "redirect:/calendario?id=" + plan.getId();
    }

    @PostMapping("/calendario/finalizar")
    public String finalizarPlan(@RequestParam("id") Long id) {
        Optional<PlanMensual> planOpt = planMensualRepository.findById(id);
        if (planOpt.isPresent()) {
            PlanMensual plan = planOpt.get();
            plan.setEstado("FINALIZADO");
            planMensualRepository.save(plan);

            List<PlanDetalle> detalles = planDetalleRepository.findByPlanMensual(plan);
            
            // Register remaining equipments that were supposed to be done but weren't
            List<Long> equiposGuardadosIds = detalles.stream().map(d -> d.getEquipo().getId()).toList();
            List<Equipo> allEquipos = equipoRepository.findAll();
            for (Equipo eq : allEquipos) {
                if (Boolean.TRUE.equals(eq.getActivo()) && "preventivo".equalsIgnoreCase(eq.getTipo()) && !equiposGuardadosIds.contains(eq.getId())) {
                    if (eq.getFechaProxima() != null && eq.getFechaProxima().getYear() == plan.getAnio() && eq.getFechaProxima().getMonthValue() == plan.getMes()) {
                        PlanDetalle noEjecutado = new PlanDetalle(plan, eq, null);
                        noEjecutado.setEstado("NO_EJECUTADO");
                        planDetalleRepository.save(noEjecutado);
                        detalles.add(noEjecutado); 
                    }
                }
            }

            for (PlanDetalle det : detalles) {
                if ("NO_EJECUTADO".equals(det.getEstado()) || "REPROGRAMADO".equals(det.getEstado())) {
                    // Si ya se reprogramó explícitamente desde el botón, la fecha ya avanzó; 
                    // Si no se reprogramó pero no se ejecutó, asume que avanzó un mes por defecto?
                    // El usuario indica: "y si lo reprograman queda como no echo", 
                    // a nivel de fecha lo auto calcularemos si no se hizo.
                    if ("NO_EJECUTADO".equals(det.getEstado())) {
                        Equipo eq = det.getEquipo();
                        String crit = eq.getCriterioProgramacion();
                        if (crit != null && !crit.trim().isEmpty()) {
                            LocalDate baseDate = LocalDate.of(plan.getAnio(), plan.getMes(), 1);
                            LocalDate nextDate = calcularSiguienteFecha(baseDate, crit);
                            eq.setFechaProxima(nextDate);
                            equipoRepository.save(eq);
                        }
                    }
                    continue; // skip the next auto-calc for reprogramados/no_ejecutados
                }

                Equipo eq = det.getEquipo();
                
                if ("correctivo".equalsIgnoreCase(eq.getTipo())) {
                    eq.setActivo(false);
                    equipoRepository.save(eq);
                    continue;
                }
                
                String crit = eq.getCriterioProgramacion();
                if (crit != null && !crit.trim().isEmpty()) {
                    try {
                        int diaOriginal = 1;
                        if (eq.getFechaProxima() != null) {
                            diaOriginal = eq.getFechaProxima().getDayOfMonth();
                        } else if (det.getDia() != null) {
                            diaOriginal = det.getDia();
                        }
                        
                        YearMonth ym = YearMonth.of(plan.getAnio(), plan.getMes());
                        diaOriginal = Math.min(diaOriginal, ym.lengthOfMonth());
                        
                        LocalDate baseDate = LocalDate.of(plan.getAnio(), plan.getMes(), diaOriginal);
                        LocalDate nextDate = calcularSiguienteFecha(baseDate, crit);
                        eq.setFechaProxima(nextDate);
                        equipoRepository.save(eq);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return "redirect:/historial";
    }

    private LocalDate calcularSiguienteFecha(LocalDate baseDate, String criterioStr) {
        if (criterioStr == null)
            return baseDate.plusMonths(1);
        String c = criterioStr.toUpperCase().trim();

        if (c.equals("MENSUAL"))
            return baseDate.plusMonths(1);
        if (c.equals("BIMESTRAL"))
            return baseDate.plusMonths(2);
        if (c.equals("TRIMESTRAL"))
            return baseDate.plusMonths(3);
        if (c.equals("CUATRIMESTRAL"))
            return baseDate.plusMonths(4);
        if (c.equals("SEMESTRAL"))
            return baseDate.plusMonths(6);
        if (c.equals("ANUAL"))
            return baseDate.plusYears(1);
        if (c.equals("QUINCENAL"))
            return baseDate.plusDays(15);
        if (c.equals("SEMANAL"))
            return baseDate.plusWeeks(1);

        int num = 1;
        String digits = c.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            try {
                num = Integer.parseInt(digits);
            } catch (Exception e) {
            }
        }

        if (c.contains("MES") || c.endsWith("M"))
            return baseDate.plusMonths(num);
        if (c.contains("AÑO") || c.contains("ANO") || c.endsWith("A") || c.endsWith("Y"))
            return baseDate.plusYears(num);
        if (c.contains("DIA") || c.contains("DÍA") || c.endsWith("D") || c.matches("^\\d+$"))
            return baseDate.plusDays(num);
        if (c.contains("SEMANA") || c.endsWith("W"))
            return baseDate.plusWeeks(num);

        // Default si no se interpreta la palabra exacta, asume suma del número en meses
        // (o 1 mes)
        return baseDate.plusMonths(num > 0 ? num : 1);
    }

    @PostMapping("/calendario/reprogramar/{id}")
    public ResponseEntity<Void> reprogramarEquipoAjax(
            @PathVariable Long id, 
            @RequestParam("fecha") String nuevaFecha, 
            @RequestParam(value = "planId", required = false, defaultValue = "0") Long planId,
            @RequestParam(value = "year", required = false, defaultValue = "0") Integer year,
            @RequestParam(value = "month", required = false, defaultValue = "0") Integer month) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo != null) {
            try {
                PlanMensual plan = null;
                
                // Si reprogramamos y había un plan activo, vinculamos esto al plan
                if (planId > 0) {
                    plan = planMensualRepository.findById(planId).orElse(null);
                } else if (year > 0 && month > 0) {
                     List<PlanMensual> existentes = planMensualRepository.findByAnioAndMes(year, month);
                     if (!existentes.isEmpty()) {
                         plan = existentes.get(0);
                     } else {
                         plan = new PlanMensual(year, month, "ACTIVO");
                         plan = planMensualRepository.save(plan);
                     }
                }
                
                if (plan != null) {
                    List<PlanDetalle> existentes = planDetalleRepository.findByPlanMensual(plan);
                    boolean yaRegistrado = existentes.stream().anyMatch(d -> d.getEquipo().getId().equals(equipo.getId()));
                    
                    if (!yaRegistrado) {
                        PlanDetalle reprogramado = new PlanDetalle(plan, equipo, null);
                        reprogramado.setEstado("REPROGRAMADO");
                        planDetalleRepository.save(reprogramado);
                    }
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                equipo.setFechaProxima(LocalDate.parse(nuevaFecha, formatter));
                equipoRepository.save(equipo);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.notFound().build();
    }
}
