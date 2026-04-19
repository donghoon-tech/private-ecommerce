package com.example.ecommerce.service;

import com.example.ecommerce.constant.AppConstants;
import com.example.ecommerce.constant.ErrorMessage;
import com.example.ecommerce.dto.UserDTO;
import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.response.LoginResponse;
import com.example.ecommerce.entity.BusinessProfile;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.DuplicateException;
import com.example.ecommerce.exception.NotFoundException;
import com.example.ecommerce.mapper.UserMapper;
import com.example.ecommerce.repository.BusinessProfileRepository;
import com.example.ecommerce.repository.RoleRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtTokenProvider;
import com.example.ecommerce.util.PhoneEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PhoneEncryptor phoneEncryptor;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTests {
        @Test
        @DisplayName("성공: 올바른 정보 입력 시 토큰과 권한 정보를 반환한다")
        void login_success() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            Authentication auth = mock(Authentication.class);
            given(auth.getName()).willReturn("testuser");
            doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("MANAGE_ORDER")
            )).when(auth).getAuthorities();

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(auth);
            given(jwtTokenProvider.createToken("testuser", "ADMIN")).willReturn("jwt-token");

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getRole()).isEqualTo("ADMIN");
            assertThat(response.getPermissions()).containsExactly("MANAGE_ORDER");
            assertThat(response.getToken()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("실패: 인증 실패 시 AuthenticationException이 발생한다 (AuthenticationManager 구현체 위임)")
        void login_fail_badCredentials() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("user");
            request.setPassword("wrong");
            given(authenticationManager.authenticate(any())).willThrow(new RuntimeException("Bad credentials"));

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Bad credentials");
        }
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class RegisterTests {
        @Test
        @DisplayName("성공: 중복 없는 정보 입력 시 회원가입을 완료하고 DTO를 반환한다")
        void register_success() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setPassword("Password123!");
            request.setPhone("010-1234-5678");
            request.setCompanyName("Test Company");
            request.setBusinessNumber("123-45-67890");

            given(userRepository.existsByUsername(anyString())).willReturn(false);
            given(phoneEncryptor.encryptForSearch("01012345678")).willReturn("encrypted-phone");
            given(userRepository.existsByRepresentativePhone("encrypted-phone")).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encoded-password");
            given(roleRepository.findByName(AppConstants.ROLE_UNVERIFIED)).willReturn(Optional.of(new Role()));
            
            User savedUser = User.builder().id(UUID.randomUUID()).username("newuser").build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toDTO(any(User.class), any(BusinessProfile.class))).willReturn(new UserDTO());

            // When
            UserDTO result = authService.register(request);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).save(any(User.class));
            verify(businessProfileRepository).save(any(BusinessProfile.class));
        }

        @Test
        @DisplayName("실패: 비밀번호 형식이 올바르지 않으면 BusinessException이 발생한다")
        void register_fail_invalidPassword() {
            RegisterRequest request = new RegisterRequest();
            request.setPassword("short");

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorMessage.PASSWORD_INVALID);
        }

        @Test
        @DisplayName("실패: 이미 존재하는 아이디인 경우 DuplicateException이 발생한다")
        void register_fail_duplicateUsername() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("existing");
            request.setPassword("Password123!");
            given(userRepository.existsByUsername("existing")).willReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateException.class)
                    .hasMessage(ErrorMessage.ID_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("실패: 이미 존재하는 휴대폰 번호인 경우 DuplicateException이 발생한다")
        void register_fail_duplicatePhone() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setPassword("Password123!");
            request.setPhone("010-1111-2222");

            given(userRepository.existsByUsername(anyString())).willReturn(false);
            given(phoneEncryptor.encryptForSearch("01011112222")).willReturn("enc");
            given(userRepository.existsByRepresentativePhone("enc")).willReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateException.class)
                    .hasMessage(ErrorMessage.PHONE_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("아이디 찾기 및 본인 확인 테스트")
    class VerificationTests {
        @Test
        @DisplayName("아이디 찾기 성공: 휴대폰 번호로 가입된 아이디를 반환한다")
        void findId_success() {
            given(phoneEncryptor.encryptForSearch("01012345678")).willReturn("enc");
            given(userRepository.findByRepresentativePhone("enc"))
                    .willReturn(Optional.of(User.builder().username("founduser").build()));

            String username = authService.findId("010-1234-5678");
            assertThat(username).isEqualTo("founduser");
        }

        @Test
        @DisplayName("아이디 찾기 실패: 일치하는 번호가 없으면 NotFoundException이 발생한다")
        void findId_notFound() {
            given(phoneEncryptor.encryptForSearch(anyString())).willReturn("enc");
            given(userRepository.findByRepresentativePhone("enc")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.findId("010-0000-0000"))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("사용자-휴대폰 일치 확인: 일치하면 true를 반환한다")
        void checkUserAndPhoneExists_match() {
            User user = User.builder().representativePhone("01012345678").build();
            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));

            boolean result = authService.checkUserAndPhoneExists("user", "010-1234-5678");
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 테스트")
    class ResetPasswordTests {
        @Test
        @DisplayName("성공: 정보가 일치하고 올바른 비밀번호 형식이면 변경을 완료한다")
        void resetPassword_success() {
            // Given
            User user = User.builder().username("user").representativePhone("01012345678").build();
            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
            given(passwordEncoder.encode("NewPass123!")).willReturn("encoded-pass");

            // When
            authService.resetPassword("user", "01012345678", "NewPass123!");

            // Then
            assertThat(user.getPasswordHash()).isEqualTo("encoded-pass");
        }

        @Test
        @DisplayName("실패: 사용자 정보와 휴대폰 번호가 매치되지 않으면 NotFoundException이 발생한다")
        void resetPassword_fail_mismatch() {
            User user = User.builder().representativePhone("01011112222").build();
            given(userRepository.findByUsername("user")).willReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resetPassword("user", "010-9999-9999", "NewPass123!"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ErrorMessage.AUTH_INFO_MISMATCH);
        }
    }
}
