package com.example.ecommerce.service;

import com.example.ecommerce.dto.RoleDTO;
import com.example.ecommerce.entity.Program;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.mapper.ProgramMapper;
import com.example.ecommerce.mapper.RoleMapper;
import com.example.ecommerce.repository.ProgramRepository;
import com.example.ecommerce.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private RoleMapper roleMapper;
    @Mock private ProgramMapper programMapper;

    @InjectMocks
    private RoleService roleService;

    @Nested
    @DisplayName("Role 생성 테스트")
    class CreateTests {
        @Test
        @DisplayName("성공: 새 Role 생성 시 이름을 대문자로 변환하여 저장한다")
        void createRole_success() {
            given(roleRepository.findByName("test_role")).willReturn(Optional.empty());
            Role savedRole = Role.builder().name("TEST_ROLE").build();
            given(roleRepository.save(any(Role.class))).willReturn(savedRole);
            given(roleMapper.toDTO(any())).willReturn(new RoleDTO());

            roleService.createRole("test_role", "Desc", null);

            verify(roleRepository, atLeastOnce()).save(argThat(r -> r.getName().equals("TEST_ROLE")));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 이름이면 예외가 발생한다")
        void createRole_fail_duplicate() {
            given(roleRepository.findByName("existing")).willReturn(Optional.of(new Role()));
            assertThatThrownBy(() -> roleService.createRole("existing", "", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("이미 존재하는 Role");
        }
    }

    @Nested
    @DisplayName("Role 수정 및 프로그램 할당 테스트")
    class UpdateAndAssignTests {
        @Test
        @DisplayName("수정: 제공된 필드(이름, 설명, 프로그램)만 덮어씌운다")
        void updateRole_partial() {
            UUID roleId = UUID.randomUUID();
            Role role = Role.builder().id(roleId).name("OLD").description("OLD DESC").programs(new HashSet<>()).build();
            given(roleRepository.findById(roleId)).willReturn(Optional.of(role));
            given(roleRepository.save(any())).willReturn(role);
            given(roleMapper.toDTO(any())).willReturn(new RoleDTO());

            // 이름은 null, 설명만 변경
            roleService.updateRole(roleId, null, "NEW DESC", null);

            assertThat(role.getName()).isEqualTo("OLD");
            assertThat(role.getDescription()).isEqualTo("NEW DESC");
        }

        @Test
        @DisplayName("프로그램 할당: 기존 프로그램 유지한 채 새로운 프로그램을 추가한다")
        void assignPrograms_success() {
            UUID roleId = UUID.randomUUID();
            Program existingProg = Program.builder().id(UUID.randomUUID()).build();
            Set<Program> progSet = new HashSet<>();
            progSet.add(existingProg);
            Role role = Role.builder().id(roleId).programs(progSet).build();

            UUID newProgId = UUID.randomUUID();
            Program newProg = Program.builder().id(newProgId).build();

            given(roleRepository.findById(roleId)).willReturn(Optional.of(role));
            given(programRepository.findAllById(anyList())).willReturn(List.of(newProg));
            given(roleRepository.save(any())).willReturn(role);
            given(roleMapper.toDTO(any())).willReturn(new RoleDTO());

            roleService.assignPrograms(roleId, List.of(newProgId));

            assertThat(role.getPrograms()).hasSize(2);
            assertThat(role.getPrograms()).contains(existingProg, newProg);
        }
    }

    @Nested
    @DisplayName("Role 삭제 테스트")
    class DeleteTests {
        @Test
        @DisplayName("성공: 일반 Role은 삭제 가능하다")
        void deleteRole_success() {
            UUID roleId = UUID.randomUUID();
            Role role = Role.builder().id(roleId).name("CUSTOM_ROLE").build();
            given(roleRepository.findById(roleId)).willReturn(Optional.of(role));

            roleService.deleteRole(roleId);

            verify(roleRepository).delete(role);
        }

        @Test
        @DisplayName("차단: 코어 Role(ADMIN, USER 등)은 삭제 시도시 예외가 발생한다")
        void deleteRole_fail_protected() {
            UUID roleId = UUID.randomUUID();
            Role admin = Role.builder().id(roleId).name("ADMIN").build();
            given(roleRepository.findById(roleId)).willReturn(Optional.of(admin));

            assertThatThrownBy(() -> roleService.deleteRole(roleId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("삭제할 수 없습니다");
        }
    }
}
