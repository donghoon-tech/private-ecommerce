package com.example.ecommerce.service;

import com.example.ecommerce.dto.UserDTO;
import com.example.ecommerce.exception.NotFoundException;
import com.example.ecommerce.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.example.ecommerce.mapper.UserMapper;
import com.example.ecommerce.dto.UserUpdateRequest;
import com.example.ecommerce.entity.BusinessProfile;
import com.example.ecommerce.entity.Role;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * 로그인한 본인의 기본 정보 및 승인된 주(Main) 사업자 프로필을 조회합니다.
     *
     * @param username 조회할 사용자 아이디
     * @return 프로필 정보가 조합된 사용자 정보 DTO
     * @throws NotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public UserDTO getMyInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        // 메인 사업자 프로필 조회 (없을 수도 있음 - 가입 초기 등)
        BusinessProfile mainProfile = businessProfileRepository.findByUserId(user.getId()).stream()
                .filter(BusinessProfile::isMain)
                .findFirst()
                .orElse(null);

        return userMapper.toDTO(user, mainProfile);
    }

    /**
     * 다건 조회 또는 타 사용자의 세부 정보를 조회할 때 사용됩니다.
     *
     * @param userId 조회 대상 사용자 식별자(UUID)
     * @return 프로필 정보가 조합된 사용자 정보 DTO
     */
    @Transactional(readOnly = true)
    public UserDTO getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        BusinessProfile mainProfile = businessProfileRepository.findByUserId(user.getId()).stream()
                .filter(BusinessProfile::isMain)
                .findFirst()
                .orElse(null);

        return userMapper.toDTO(user, mainProfile);
    }

    /**
     * 로그인한 본인의 정보 수정(비밀번호, 전화번호, 이메일 및 연관 메인 사업자 프로필 일부 정보)을 처리합니다.
     *
     * @param username 정보를 갱신할 대상 본인 계정 아이디
     * @param request 갱신할 필드들을 담고 있는 요청 객체
     * @return 정보가 성공적으로 갱신된 이후의 종합 정보 DTO
     */
    public UserDTO updateMyInfo(String username, UserUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        // 유저 기본 정보 수정
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            ValidationUtils.validatePassword(request.getPassword());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getName() != null)
            user.setName(request.getName());
        if (request.getRepresentativePhone() != null)
            user.setRepresentativePhone(ValidationUtils.normalizePhone(request.getRepresentativePhone()));
        if (request.getEmail() != null)
            user.setEmail(request.getEmail());

        // 사업자 정보 수정 (메인 프로필이 존재할 경우)
        if (request.getCompanyName() != null || request.getOfficeAddress() != null) {
            businessProfileRepository.findByUserId(user.getId()).stream()
                    .filter(BusinessProfile::isMain)
                    .findFirst()
                    .ifPresent(profile -> {
                        if (request.getCompanyName() != null)
                            profile.setBusinessName(request.getCompanyName());
                        if (request.getOfficeAddress() != null)
                            profile.setOfficeAddress(request.getOfficeAddress());
                        if (request.getStorageAddress() != null)
                            profile.setStorageAddress(request.getStorageAddress());
                    });
        }

        return getMyInfo(username); // 수정된 정보 반환
    }

    /**
     * 시스템 상의 전체 가입 사용자 목록과 메인 사업자 정보를 추출하는 관리자 전용 기능입니다.
     *
     * @return 연관 프로필을 갖춘 사용자 정보 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsersWithProfiles() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> {
                    BusinessProfile mainProfile = businessProfileRepository.findByUserId(user.getId()).stream()
                            .filter(BusinessProfile::isMain)
                            .findFirst()
                            .orElse(null);
                    return userMapper.toDTO(user, mainProfile);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 관리 대상 사용자의 권한 등급(Role)을 강제로 변경합니다.
     *
     * @param userId 등급 조정 대상의 사용자 ID
     * @param roleStr 강제 이관시킬 Role 이름 문자열 (예: ADMIN)
     * @return 변경 반영된 사용자 DTO
     */
    public UserDTO updateUserRole(UUID userId, String roleStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        Role newRole = roleRepository.findByName(roleStr.toUpperCase())
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));
        user.setRole(newRole);

        BusinessProfile mainProfile = businessProfileRepository.findByUserId(user.getId()).stream()
                .filter(BusinessProfile::isMain)
                .findFirst()
                .orElse(null);

        return userMapper.toDTO(user, mainProfile);
    }

    /**
     * 가입 직후 '승인 대기(PENDING)' 구간에 머물러 있는 사용자의 사업자 프로필을 '승인 처리' 합니다.
     * 승인이 완료되면 해당 사용자의 계정 권한이 기본 미검증(UNVERIFIED)에서 승인 회원(USER)으로 자동 승격되는 특이사항이 있습니다.
     *
     * @param profileId 승인 대상이 되는 사업자 프로필 식별자
     * @param adminUsername 처리자(액션을 취한 관리자) 정보 기록용
     */
    public void approveBusinessProfile(UUID profileId, String adminUsername) {
        BusinessProfile profile = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.PROFILE_NOT_FOUND));

        if (profile.getStatus() != BusinessProfile.Status.PENDING) {
            throw new BusinessException(ErrorMessage.ONLY_PENDING_APPROVABLE);
        }

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ADMIN_NOT_FOUND));

        // 1. 프로필 상태 변경
        profile.setStatus(BusinessProfile.Status.APPROVED);
        profile.setApprovedAt(LocalDateTime.now());
        profile.setApprovedBy(admin);
        profile.setRejectionReason(null);

        // 2. 사용자 역할 변경 (UNVERIFIED -> USER)
        User user = profile.getUser();
        Role userRole = roleRepository.findByName(AppConstants.ROLE_USER)
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "USER 역할을 찾을 수 없습니다."));
        user.setRole(userRole);
    }

    /**
     * 사업자 프로필 승인 요청에 대해 가입 불가 사유와 함께 '반려(REJECTED)' 처리합니다.
     *
     * @param profileId 반려할 사업자 프로필의 식별자
     * @param reason 프로필 승인 거절 구체적 사유
     */
    public void rejectBusinessProfile(UUID profileId, String reason) {
        BusinessProfile profile = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.PROFILE_NOT_FOUND));

        if (profile.getStatus() != BusinessProfile.Status.PENDING) {
            throw new BusinessException(ErrorMessage.ONLY_PENDING_REJECTABLE);
        }

        profile.setStatus(BusinessProfile.Status.REJECTED);
        profile.setRejectionReason(reason);
    }
}
