package com.planer.planner.controller;

import com.planer.planner.model.Equipo;
import com.planer.planner.repository.EquipoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        List<Equipo> equiposBase = equipoRepository.findByTipoNotIgnoreCaseOrTipoIsNull("correctivo");
        model.addAttribute("equipos", correctivos);
        model.addAttribute("equiposBase", equiposBase);
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

    @GetMapping("/equipos/excel/template")
    public ResponseEntity<byte[]> descargarPlantilla() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Equipos");
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Tag", "Descripción", "Criterio Programación", "Taller", "Tipo", "Buque", "Mes Inicial", "Fecha Próxima (YYYY-MM-DD)", "Duración", "Criticidad (0 o 1)"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla_equipos.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(outputStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/equipos/excel/upload")
    public String cargarExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "redirect:/equipos?error=empty_file";
        }
        
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Equipo equipo = new Equipo();
                
                // Tag
                Cell cell = row.getCell(0);
                if (cell == null || cell.getCellType() == CellType.BLANK) continue; 
                equipo.setTag(getCellValueAsString(cell));
                
                // Descripción
                equipo.setDescripcion(getCellValueAsString(row.getCell(1)));
                
                // Criterio Programación
                equipo.setCriterioProgramacion(getCellValueAsString(row.getCell(2)));
                
                // Taller
                equipo.setTaller(getCellValueAsString(row.getCell(3)));
                
                // Tipo
                String tipo = getCellValueAsString(row.getCell(4));
                equipo.setTipo(tipo.isEmpty() ? "preventivo" : tipo);
                
                // Buque
                String buqueStr = getCellValueAsString(row.getCell(5));
                if (!buqueStr.isEmpty()) {
                    try { equipo.setBuque(Double.parseDouble(buqueStr)); } catch (NumberFormatException ignored) {}
                }
                
                // Mes Inicial
                equipo.setMesInicial(getCellValueAsString(row.getCell(6)));
                
                // Fecha Próxima
                Cell fechaCell = row.getCell(7);
                if (fechaCell != null) {
                    if (fechaCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(fechaCell)) {
                        equipo.setFechaProxima(fechaCell.getLocalDateTimeCellValue().toLocalDate());
                    } else {
                        String fechaStr = getCellValueAsString(fechaCell);
                        if (!fechaStr.isEmpty()) {
                            try { equipo.setFechaProxima(LocalDate.parse(fechaStr, formatter)); } catch (Exception ignored) {}
                        }
                    }
                }
                
                // Duración
                equipo.setDuracion(getCellValueAsString(row.getCell(8)));
                
                // Criticidad
                String criticidadStr = getCellValueAsString(row.getCell(9));
                if (!criticidadStr.isEmpty()) {
                    try { equipo.setCriticidad((int) Double.parseDouble(criticidadStr)); } catch (NumberFormatException ignored) {}
                } else {
                    equipo.setCriticidad(0);
                }
                
                equipo.setActivo(true);
                equipoRepository.save(equipo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/equipos?error=upload_failed";
        }
        
        return "redirect:/equipos?success=upload_ok";
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: 
                if (DateUtil.isCellDateFormatted(cell)) return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }
}
