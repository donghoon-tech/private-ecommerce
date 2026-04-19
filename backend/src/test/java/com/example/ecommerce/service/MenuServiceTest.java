package com.example.ecommerce.service;

import com.example.ecommerce.dto.MenuDTO;
import com.example.ecommerce.entity.Menu;
import com.example.ecommerce.entity.Program;
import com.example.ecommerce.repository.MenuRepository;
import com.example.ecommerce.repository.ProgramRepository;
import com.example.ecommerce.repository.UserRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock private MenuRepository menuRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private MenuService menuService;

    @Nested
    @DisplayName("사용자별 메뉴 트리 조회 테스트 (권한 필터링 및 가지치기)")
    class UserMenuTests {
        @Test
        @DisplayName("게스트 조회: PUBLIC 프로그램이거나 유효한 직접 경로가 있는 메뉴만 포함한다")
        void getUserMenuTree_guest() {
            // Given: Root(Visible) -> Sub1(Public Program), Sub2(Private Program), Sub3(Direct Path)
            Menu root = Menu.builder().id(UUID.randomUUID()).name("Root").isVisible(true).build();
            Program publicProg = Program.builder().id(UUID.randomUUID()).isPublic(true).url("/public").build();
            Menu sub1 = Menu.builder().id(UUID.randomUUID()).parent(root).program(publicProg).isVisible(true).build();
            
            Program privateProg = Program.builder().id(UUID.randomUUID()).isPublic(false).url("/private").build();
            Menu sub2 = Menu.builder().id(UUID.randomUUID()).parent(root).program(privateProg).isVisible(true).build();
            
            Menu sub3 = Menu.builder().id(UUID.randomUUID()).parent(root).path("/direct").isVisible(true).build();

            given(menuRepository.findAllWithProgram()).willReturn(List.of(root, sub1, sub2, sub3));

            // When
            List<MenuDTO> result = menuService.getUserMenuTree(null);

            // Then
            assertThat(result).hasSize(1);
            MenuDTO rootDTO = result.get(0);
            // sub1(public), sub3(direct)은 포함되나, sub2(private)는 게스트에게 보이지 않아야 함
            assertThat(rootDTO.getChildren()).hasSize(2);
            assertThat(rootDTO.getChildren())
                    .extracting(MenuDTO::getId)
                    .containsExactlyInAnyOrder(sub1.getId(), sub3.getId());
        }

        @Test
        @DisplayName("가지치기(Pruning): 자식 메뉴가 하나도 남지 않은 상위 메뉴는 트리에서 제외된다")
        void getUserMenuTree_pruning() {
            // Root -> Empty Folder (No children after filtering)
            Menu root = Menu.builder().id(UUID.randomUUID()).name("Folder").isVisible(true).build();
            Program privateProg = Program.builder().id(UUID.randomUUID()).isPublic(false).build();
            Menu sub = Menu.builder().id(UUID.randomUUID()).parent(root).program(privateProg).isVisible(true).build();

            given(menuRepository.findAllWithProgram()).willReturn(List.of(root, sub));

            List<MenuDTO> result = menuService.getUserMenuTree(null);

            // sub가 잘려나갔으므로 자식이 없는 root도 가지치기 되어야 함
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("메뉴 관리 테스트")
    class ManageTests {
        @Test
        @DisplayName("생성 시 Path 미지정 후 WEB 프로그램 연결 시 프로그램의 URL을 상속받는다")
        void createMenu_pathInheritance() {
            // This is actually tested during tree building/DTO conversion
            Menu root = Menu.builder().id(UUID.randomUUID()).name("Root").isVisible(true).build();
            Program webProg = Program.builder().id(UUID.randomUUID()).url("/auto-path").type(com.example.ecommerce.entity.ProgramType.WEB).isPublic(true).build();
            Menu sub = Menu.builder().id(UUID.randomUUID()).parent(root).program(webProg).isVisible(true).build();

            given(menuRepository.findAllWithProgram()).willReturn(List.of(root, sub));

            List<MenuDTO> result = menuService.getAllMenuTree();

            assertThat(result.get(0).getChildren().get(0).getPath()).isEqualTo("/auto-path");
        }

        @Test
        @DisplayName("수정 실패: 본인을 부모로 지정하면 예외가 발생한다")
        void updateMenu_fail_selfReference() {
            UUID id = UUID.randomUUID();
            given(menuRepository.findById(id)).willReturn(Optional.of(Menu.builder().id(id).build()));

            assertThatThrownBy(() -> menuService.updateMenu(id, id, null, "Name", null, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot be its own parent");
        }
    }
}
