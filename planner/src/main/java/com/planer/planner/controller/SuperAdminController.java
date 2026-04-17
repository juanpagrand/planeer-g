package com.planer.planner.controller;

import com.planer.planner.model.Admin;
import com.planer.planner.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String index(Model model) {
        List<Admin> admins = adminRepository.findAll();
        model.addAttribute("admins", admins);
        return "superadmin";
    }

    @PostMapping("/usuarios")
    public String crearUsuario(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String role) {
        if (adminRepository.findByUsername(username).isEmpty()) {
            Admin newAdmin = new Admin(
                    username,
                    passwordEncoder.encode(password),
                    role
            );
            adminRepository.save(newAdmin);
        }
        return "redirect:/superadmin";
    }

    @PostMapping("/usuarios/editar")
    public String editarUsuario(@RequestParam Long id,
                                @RequestParam String username,
                                @RequestParam(required = false) String password,
                                @RequestParam String role) {
        adminRepository.findById(id).ifPresent(admin -> {
            admin.setUsername(username);
            admin.setRole(role);
            if (password != null && !password.trim().isEmpty()) {
                admin.setPassword(passwordEncoder.encode(password));
            }
            adminRepository.save(admin);
        });
        return "redirect:/superadmin";
    }

    @PostMapping("/usuarios/eliminar")
    public String eliminarUsuario(@RequestParam Long id) {
        adminRepository.deleteById(id);
        return "redirect:/superadmin";
    }
}
