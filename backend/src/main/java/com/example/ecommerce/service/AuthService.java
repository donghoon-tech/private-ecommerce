package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.exception.DuplicateException;
import com.example.ecommerce.exception.NotFoundException;
import com.example.ecommerce.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.example.ecommerce.dto.UserDTO;
import com.example.ecommerce.mapper.UserMapper;
import com.example.ecommerce.entity.BusinessProfile;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.BusinessProfileRepository;
import com.example.ecommerce.repository.RoleRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ecommerce.util.ValidationUtils;
import com.example.ecommerce.constant.AppConstants;
import com.example.ecommerce.constant.ErrorMessage;
import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.response.LoginResponse;
import com.example.ecommerce.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        String username = authentication.getName();

        // ROLE_ 로 시작하는 권한(역할) 찾기
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith(AppConstants.ROLE_PREFIX))
                .findFirst()
                .map(auth -> auth.replace(AppConstants.ROLE_PREFIX, ""))
                .orElse(AppConstants.ROLE_USER);

        // 역할(Role)을 제외한 나머지 모든 실제 액션 퍼미션 수집
        List<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith(AppConstants.ROLE_PREFIX))
                .collect(Collectors.toList());

        String token = jwtTokenProvider.createToken(username, role);

        return LoginResponse.builder()
                .username(username)
                .role(role)
                .permissions(permissions)
                .token(token)
                .build();
    }

    public UserDTO register(RegisterRequest request) {
        // 비밀번호 복잡성 검증
        ValidationUtils.validatePassword(request.getPassword());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateException(ErrorMessage.ID_ALREADY_EXISTS);
        }
        String normalizedPhone = normalizePhone(request.getPhone());
        if (userRepository.existsByRepresentativePhone(normalizedPhone)) {
            throw new DuplicateException(ErrorMessage.PHONE_ALREADY_EXISTS);
        }

        // 1. 사용자 계정 생성
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getCompanyName()) // 초기값: 상호명을 이름으로 사용? or 별도 이름 필드 필요? (일단 DTO 확인 필요) -> 아 request 필드 보니
                                                // name이 없었음.
                .representativePhone(normalizedPhone)
                .email(request.getEmail())
                .role(roleRepository.findByName(AppConstants.ROLE_UNVERIFIED)
                        .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "기본 권한을 찾을 수 없습니다.")))
                .businessNumber(request.getBusinessNumber())
                .isActive(true)
                .build();

        // request에 name(대표자명) 필드가 없다면, 회원가입 폼을 수정해야 할 수도 있음.
        // 하지만 기존 코드: companyName을 사용.
        // 새 스키마 User.name (대표자명), BusinessProfile.businessName (상호명),
        // BusinessProfile.representativeName (대표자명)
        // 일단 request의 companyName을 상호명으로, User.name은 '대표자'로 가정하고 매핑하거나, AuthController
        // 수정이 필요해 보임.
        // 여기서는 임시로 companyName을 User.name과 BusinessProfile.businessName 둘 다에 넣겠습니다.
        // (정확한 매핑을 위해선 AuthController.RegisterRequest 수정 필요)

        user.setName(request.getCompanyName()); // 임시

        User savedUser = userRepository.save(user);

        // 2. 사업자 프로필 생성 (Pending 상태)
        BusinessProfile profile = BusinessProfile.builder()
                .user(savedUser)
                .businessName(request.getCompanyName())
                .businessNumber(request.getBusinessNumber())
                .representativeName(request.getCompanyName()) // 대표자명 필드 부재로 상호명 사용
                .officeAddress(request.getBusinessAddress())
                .storageAddress(request.getYardAddress())
                .status(BusinessProfile.Status.pending)
                .isMain(true) // 첫 가입 시 주 사업자로 설정
                .build();

        businessProfileRepository.save(profile);

        // 3. 반환
        return userMapper.toDTO(savedUser, profile);
    }

    public String findId(String phone) {
        return userRepository.findByRepresentativePhone(normalizePhone(phone))
                .map(User::getUsername)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.AUTH_INFO_MISMATCH));
    }

    public boolean checkPhoneExists(String phone) {
        return userRepository.existsByRepresentativePhone(normalizePhone(phone));
    }

    public boolean checkUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean checkUserAndPhoneExists(String username, String phone) {
        String cleanInputPhone = normalizePhone(phone);
        return userRepository.findByUsername(username)
                .map(u -> {
                    String cleanUserPhone = normalizePhone(u.getRepresentativePhone());
                    return cleanUserPhone.equals(cleanInputPhone);
                })
                .orElse(false);
    }

    public String resetPassword(String username, String phone) {
        String cleanInputPhone = normalizePhone(phone);
        User user = userRepository.findByUsername(username)
                .filter(u -> {
                    String cleanUserPhone = normalizePhone(u.getRepresentativePhone());
                    return cleanUserPhone.equals(cleanInputPhone);
                })
                .orElseThrow(() -> new NotFoundException(ErrorMessage.AUTH_INFO_MISMATCH));

        String tempPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        // Dirty Checking
        return tempPassword;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    private String normalizePhone(String phone) {
        if (phone == null)
            return null;
        return phone.replaceAll("[^0-9]", "");
    }
}