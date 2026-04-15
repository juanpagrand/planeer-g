package com.planer.planner.security;

import com.planer.planner.model.Admin;
import com.planer.planner.repository.AdminRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    public CustomUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        
        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPassword())
                .roles(admin.getRole().replace("ROLE_", "")) // Spring Security adds ROLE_ prefix internally
                .build();
    }
}
