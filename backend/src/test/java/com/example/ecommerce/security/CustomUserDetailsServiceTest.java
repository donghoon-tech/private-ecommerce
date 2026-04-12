package com.example.ecommerce.security;

import com.example.ecommerce.entity.Program;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername 호출 시 User의 Role과 연관된 Program 정보가 Authorities로 변환된다")
    void loadUserByUsername_success() {
        // Given
        Program program = Program.builder()
                .programCode("PG_PRODUCT")
                .name("상품 관리")
                .build();
                
        Role role = Role.builder()
                .name("USER")
                .programs(Set.of(program))
                .build();
        
        User user = User.builder()
                .username("testuser")
                .passwordHash("hashedPwd")
                .role(role)
                .build();

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getAuthorities()).hasSize(3); // ROLE_USER, PG_PRODUCT, PG_PRODUCT:READ
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "PG_PRODUCT", "PG_PRODUCT:READ");
    }

    @Test
    @DisplayName("존재하지 않는 User 조회 시 예외가 발생한다")
    void loadUserByUsername_notFound() {
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username: unknown");
    }
}
