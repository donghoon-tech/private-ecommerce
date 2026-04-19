package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProgramDTO;
import com.example.ecommerce.entity.Program;
import com.example.ecommerce.mapper.ProgramMapper;
import com.example.ecommerce.repository.ProgramRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock private ProgramRepository programRepository;
    @Mock private ProgramMapper programMapper;

    @InjectMocks
    private ProgramService programService;

    @Test
    @DisplayName("캐시 갱신: DB의 프로그램 정보를 바탕으로 URL 매핑 맵을 구성한다")
    void refreshCache_logic() {
        // Given
        Program p1 = Program.builder()
                .programCode("P1")
                .url("/api/v1/test")
                .httpMethod("GET")
                .isPublic(true)
                .build();
        Program p2 = Program.builder()
                .programCode("P2")
                .url("/api/v1/save")
                .httpMethod("POST")
                .isPublic(false)
                .build();
        Program p3 = Program.builder()
                .programCode("P3")
                .url("/api/v1/any")
                .httpMethod(null) // Should default to ANY
                .build();

        given(programRepository.findAll()).willReturn(List.of(p1, p2, p3));

        // When
        programService.refreshCache();

        // Then
        Map<String, ProgramService.ProgramCacheInfo> map = programService.getUrlProgramMap();
        assertThat(map).hasSize(3);
        assertThat(map.get("GET /api/v1/test").getProgramCode()).isEqualTo("P1");
        assertThat(map.get("GET /api/v1/test").isPublic()).isTrue();
        
        assertThat(map.get("POST /api/v1/save").getProgramCode()).isEqualTo("P2");
        assertThat(map.get("POST /api/v1/save").isPublic()).isFalse();
        
        assertThat(map.get("ANY /api/v1/any").getProgramCode()).isEqualTo("P3");
    }

    @Test
    @DisplayName("변경 감지: 프로그램 저장 시 캐시를 즉시 갱신한다")
    void saveProgram_updatesCache() {
        ProgramDTO dto = new ProgramDTO();
        Program entity = Program.builder().id(UUID.randomUUID()).build();
        
        given(programMapper.toEntity(dto)).willReturn(entity);
        given(programRepository.save(entity)).willReturn(entity);
        given(programMapper.toDTO(entity)).willReturn(dto);

        programService.saveProgram(dto);

        // save 후 refreshCache가 호출되어 findAll이 실행되어야 함
        verify(programRepository).findAll();
    }
}
