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
import com.example.ecommerce.util.PhoneEncryptor;
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
    private final PhoneEncryptor phoneEncryptor;

    /**
     * 사용자 로그인을 처리하고 JWT 토큰을 발급합니다.
     * 인증 정보에서 'ROLE_' 접두사가 붙은 권한(Role)과 그 외 세부 권한(Permissions)을 분리하여 반환하는 특이사항이 있습니다.
     *
     * @param loginRequest 사용자가 입력한 아이디와 비밀번호
     * @return 사용자명, 권한, 세부 권한 목록 및 JWT 토큰을 포함한 로그인 응답
     */
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

    /**
     * 신규 사용자를 회원가입 처리하고 연관된 사업자 프로필을 '승인 대기(PENDING)' 상태로 일괄 생성합니다.
     * 비밀번호 복잡성 검증 및 휴대폰 번호 암호화 기반 중복 검증 로직이 포함되어 있습니다.
     *
     * @param request 회원가입 요청 정보 (아이디, 비밀번호, 상호명, 대표번호 등)
     * @return 가입 완료된 사용자 정보
     * @throws DuplicateException 아이디 혹은 전화번호가 이미 존재하는 경우
     */
    public UserDTO register(RegisterRequest request) {
        // 비밀번호 복잡성 검증
        ValidationUtils.validatePassword(request.getPassword());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateException(ErrorMessage.ID_ALREADY_EXISTS);
        }
        String normalizedPhone = ValidationUtils.normalizePhone(request.getPhone());
        if (userRepository.existsByRepresentativePhone(phoneEncryptor.encryptForSearch(normalizedPhone))) {
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
                .status(BusinessProfile.Status.PENDING)
                .isMain(true) // 첫 가입 시 주 사업자로 설정
                .build();

        businessProfileRepository.save(profile);

        // 3. 반환
        return userMapper.toDTO(savedUser, profile);
    }

    /**
     * 등록된 휴대폰 번호로 사용자 아이디(username)를 찾습니다.
     * 보안을 위해 휴대폰 번호를 정규화 및 암호화하여 데이터베이스에서 조회합니다.
     *
     * @param phone 찾고자 하는 가입자의 휴대폰 번호
     * @return 일치하는 사용자 아이디
     * @throws NotFoundException 일치하는 정보가 없을 경우
     */
    public String findId(String phone) {
        String normalizedPhone = ValidationUtils.normalizePhone(phone);
        return userRepository.findByRepresentativePhone(phoneEncryptor.encryptForSearch(normalizedPhone))
                .map(User::getUsername)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.AUTH_INFO_MISMATCH));
    }

    /**
     * 입력된 휴대폰 번호로 이미 가입된 회원이 있는지 확인합니다.
     * 암호화 검색이 지원되도록 번호를 암호화한 뒤 DB를 조회합니다.
     *
     * @param phone 검사할 휴대폰 번호
     * @return 회원 존재 여부 (존재하면 true)
     */
    public boolean checkPhoneExists(String phone) {
        return userRepository.existsByRepresentativePhone(
                phoneEncryptor.encryptForSearch(ValidationUtils.normalizePhone(phone)));
    }

    /**
     * 입력된 사용자 아이디(username)가 이미 존재하는지 확인합니다.
     *
     * @param username 검사할 사용자 아이디
     * @return 중복 여부 (존재하면 true)
     */
    public boolean checkUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 사용자 아이디와 휴대폰 번호가 일치하는 계정이 존재하는지 확인합니다.
     * 비밀번호 재설정 전 본인 확인용 등으로 사용됩니다.
     *
     * @param username 검증할 사용자 아이디
     * @param phone 검증할 휴대폰 번호
     * @return 일치하는 계정 존재 여부 (존재 시 true)
     */
    public boolean checkUserAndPhoneExists(String username, String phone) {
        String cleanInputPhone = ValidationUtils.normalizePhone(phone);
        return userRepository.findByUsername(username)
                .map(u -> cleanInputPhone.equals(
                        ValidationUtils.normalizePhone(u.getRepresentativePhone())))
                .orElse(false);
    }

    /**
     * 대상 사용자의 정보를 검증한 후 새로운 비밀번호로 변경(재설정)합니다.
     * 아이디와 휴대폰 번호가 모두 일치해야 하며, 새 비밀번호의 복잡성 검증을 필수적으로 거칩니다.
     *
     * @param username 대상 사용자 아이디
     * @param phone 해당 사용자의 등록된 휴대폰 번호 (본인 인증 용도)
     * @param newPassword 변경할 새로운 비밀번호
     * @throws NotFoundException 계정 정보가 일치하지 않거나 존재하지 않을 경우
     */
    public void resetPassword(String username, String phone, String newPassword) {
        String cleanInputPhone = ValidationUtils.normalizePhone(phone);
        User user = userRepository.findByUsername(username)
                .filter(u -> cleanInputPhone.equals(
                        ValidationUtils.normalizePhone(u.getRepresentativePhone())))
                .orElseThrow(() -> new NotFoundException(ErrorMessage.AUTH_INFO_MISMATCH));

        ValidationUtils.validatePassword(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

}