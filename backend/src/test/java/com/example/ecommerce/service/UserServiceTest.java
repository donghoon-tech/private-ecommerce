package com.example.ecommerce.service;

import com.example.ecommerce.dto.UserDTO;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.mapper.UserMapper;
import com.example.ecommerce.repository.BusinessProfileRepository;
import com.example.ecommerce.repository.RoleRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.entity.BusinessProfile;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BusinessProfileRepository businessProfileRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("관리자가 사용자의 역할을 변경하면 DB의 RoleRepository를 조회하여 부여한다")
    void updateUserRole_success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("testuser").role(new Role()).build();
        Role newRole = Role.builder().name("ADMIN").build();
        UserDTO expectedDTO = UserDTO.builder().username("testuser").role("ADMIN").build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(roleRepository.findByName("ADMIN")).willReturn(Optional.of(newRole));
        given(userMapper.toDTO(any(User.class), any())).willReturn(expectedDTO);

        // When
        UserDTO result = userService.updateUserRole(userId, "admin");

        // Then
        assertThat(result.getRole()).isEqualTo("ADMIN");
        assertThat(user.getRole().getName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("사업자 프로필 승인 시 프로필 상태가 approved로 변경되고 사용자 권한이 USER로 격상된다")
    void approveBusinessProfile_success() {
        // Given
        UUID profileId = UUID.randomUUID();
        String adminUsername = "adminUser";
        
        User admin = User.builder().username(adminUsername).build();
        User seller = User.builder().username("seller").build();
        BusinessProfile profile = BusinessProfile.builder()
                .id(profileId)
                .user(seller)
                .status(BusinessProfile.Status.PENDING)
                .build();
        Role userRole = Role.builder().name("USER").build();

        given(businessProfileRepository.findById(profileId)).willReturn(Optional.of(profile));
        given(userRepository.findByUsername(adminUsername)).willReturn(Optional.of(admin));
        given(roleRepository.findByName("USER")).willReturn(Optional.of(userRole));

        // When
        userService.approveBusinessProfile(profileId, adminUsername);

        // Then
        assertThat(profile.getStatus()).isEqualTo(BusinessProfile.Status.APPROVED);
        assertThat(profile.getApprovedBy()).isEqualTo(admin);
        assertThat(seller.getRole().getName()).isEqualTo("USER");
    }

    @Test
    @DisplayName("이미 승인된 프로필을 다시 승인하려 하면 예외가 발생한다")
    void approveBusinessProfile_alreadyProcessed_fail() {
        // Given
        UUID profileId = UUID.randomUUID();
        BusinessProfile profile = BusinessProfile.builder()
                .id(profileId)
                .status(BusinessProfile.Status.APPROVED) // 이미 승인됨
                .build();

        given(businessProfileRepository.findById(profileId)).willReturn(Optional.of(profile));

        // When & Then
        assertThatThrownBy(() -> userService.approveBusinessProfile(profileId, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("대기 상태인 프로필만 승인할 수 있습니다.");
    }
}
