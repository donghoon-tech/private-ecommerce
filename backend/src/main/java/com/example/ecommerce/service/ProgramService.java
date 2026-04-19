package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProgramDTO;
import com.example.ecommerce.mapper.ProgramMapper;
import com.example.ecommerce.entity.Program;
import com.example.ecommerce.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramService {

    @lombok.Value
    public static class ProgramCacheInfo {
        String programCode;
        boolean isPublic;
    }

    private final ProgramRepository programRepository;
    private final ProgramMapper programMapper;
    
    // URL 패턴별 필요한 ProgramCode를 저장하는 캐시
    // Key: "METHOD /url/path" (예: "GET /api/products")
    // TODO: 서버 다중화 시 데이터 정합성을 위해 Redis Pub/Sub 또는 공유 저장소(Redis) 기반으로 전환 필요
    private final Map<String, ProgramCacheInfo> urlProgramMap = new ConcurrentHashMap<>();

    /**
     * 스프링 빈(Bean) 초기화 시점에 프로그램별 URL 권한 캐시를 최초 로드하는 특이사항이 있습니다.
     */
    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * 데이터베이스에 저장된 최신 프로그램 정보를 기반으로 URL 권한 캐시(urlProgramMap)를 갱신합니다.
     * (현재 다중 서버 환경 구성 전, ConcurrentHashMap을 임시 캐시로 운영 중인 특이사항이 있습니다.)
     */
    @Transactional(readOnly = true)
    public void refreshCache() {
        List<Program> programs = programRepository.findAll();
        urlProgramMap.clear();
        for (Program program : programs) {
            if (program.getUrl() != null && !program.getUrl().isEmpty()) {
                String method = program.getHttpMethod() != null ? program.getHttpMethod().toUpperCase() : "ANY";
                String key = method + " " + program.getUrl();
                urlProgramMap.put(key, new ProgramCacheInfo(program.getProgramCode(), program.isPublic()));
            }
        }
        log.info("Loaded {} program URL mappings into cache", urlProgramMap.size());
    }

    /**
     * 현재 메모리에 캐싱되어 있는 웹 메서드-URL 별 프로그램 권한 매핑 정보를 반환합니다.
     *
     * @return HTTP Method 및 경로 기준으로 매핑된 캐시 정보 맵
     */
    public Map<String, ProgramCacheInfo> getUrlProgramMap() {
        return urlProgramMap;
    }
    
    /**
     * 시스템에 등록된 전체 프로그램(접근 제어 단위) 목록을 조회합니다.
     *
     * @return 전체 프로그램 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ProgramDTO> getAllPrograms() {
        return programRepository.findAll().stream()
                .map(programMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 새로운 프로그램을 저장하거나 기존 프로그램을 업데이트합니다.
     * 인가(Authorization) 데이터 변경이 일어나므로 캐시 싱크를 위해 refreshCache()를 즉시 호출하는 특이사항이 있습니다.
     *
     * @param dto 반영할 프로그램 정보 DTO
     * @return 정상 저장 및 갱신된 프로그램 정보
     */
    @Transactional
    public ProgramDTO saveProgram(ProgramDTO dto) {
        Program program = programMapper.toEntity(dto);
        Program saved = programRepository.save(program);
        refreshCache(); // 변경 시 캐시 갱신
        return programMapper.toDTO(saved);
    }

    /**
     * 지정된 식별자의 프로그램을 삭제합니다.
     * 삭제 후 인가 권한 변경사항 반영을 위해 즉시 캐시(refreshCache)를 초기화 및 갱신합니다.
     *
     * @param id 삭제 대상 프로그램의 식별자(UUID)
     */
    @Transactional
    public void deleteProgram(java.util.UUID id) {
        programRepository.deleteById(id);
        refreshCache(); // 삭제 시 캐시 갱신
    }
}
