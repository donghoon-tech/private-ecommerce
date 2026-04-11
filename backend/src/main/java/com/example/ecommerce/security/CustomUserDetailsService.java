package com.example.ecommerce.security;


import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Attempting to load user: " + username);
        com.example.ecommerce.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("User not found: " + username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        System.out.println("User found: " + user.getUsername() + ", Role: " + (user.getRole() != null ? user.getRole().getName() : "NULL"));

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().toUpperCase()));
            System.out.println("Added role authority: ROLE_" + user.getRole().getName().toUpperCase());

            if (user.getRole().getPrograms() != null) {
                user.getRole().getPrograms().forEach(program -> {
                    if (program.getProgramCode() != null) {
                        String c = program.getProgramCode().toUpperCase();
                        authorities.add(new SimpleGrantedAuthority(c)); // We add the program code directly
                        authorities.add(new SimpleGrantedAuthority(c + ":READ")); // For backward compatibility if needed
                    }
                });
            } else {
                 System.out.println("Permissions are null for role: " + user.getRole().getName());
            }
        }

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
