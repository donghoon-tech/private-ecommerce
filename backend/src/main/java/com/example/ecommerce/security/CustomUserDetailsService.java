package com.example.ecommerce.security;

import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("User not found: " + username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        System.out.println("User found: " + user.getUsername() + ", Role: " + (user.getRole() != null ? user.getRole().getName() : "NULL"));

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().toUpperCase()));
            System.out.println("Added role authority: ROLE_" + user.getRole().getName().toUpperCase());

            if (user.getRole().getMenuActions() != null) {
                user.getRole().getMenuActions().forEach(action -> {
                    if (action.getMenu() != null && action.getMenu().getMenuCode() != null) {
                        String c = action.getMenu().getMenuCode().toUpperCase();
                        if (action.isCanRead()) authorities.add(new SimpleGrantedAuthority(c + ":READ"));
                        if (action.isCanCreate()) authorities.add(new SimpleGrantedAuthority(c + ":CREATE"));
                        if (action.isCanUpdate()) authorities.add(new SimpleGrantedAuthority(c + ":UPDATE"));
                        if (action.isCanDelete()) authorities.add(new SimpleGrantedAuthority(c + ":DELETE"));
                        if (action.isCanExcel()) authorities.add(new SimpleGrantedAuthority(c + ":EXCEL"));
                    }
                });
            } else {
                 System.out.println("Permissions are null for role: " + user.getRole().getName());
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                authorities);
    }
}
