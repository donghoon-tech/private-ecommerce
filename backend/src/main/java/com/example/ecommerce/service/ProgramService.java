package com.example.ecommerce.service;

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

    private final ProgramRepository programRepository;
    
    // URL 패턴별 필요한 ProgramCode를 저장하는 캐시
    // Key: "METHOD /url/path" (예: "GET /api/products")
    // TODO: 서버 다중화 시 데이터 정합성을 위해 Redis Pub/Sub 또는 공유 저장소(Redis) 기반으로 전환 필요
    private final Map<String, String> urlProgramMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Transactional(readOnly = true)
    public void refreshCache() {
        List<Program> programs = programRepository.findAll();
        urlProgramMap.clear();
        for (Program program : programs) {
            if (program.getUrl() != null && !program.getUrl().isEmpty()) {
                String method = program.getHttpMethod() != null ? program.getHttpMethod().toUpperCase() : "ANY";
                String key = method + " " + program.getUrl();
                urlProgramMap.put(key, program.getProgramCode());
            }
        }
        log.info("Loaded {} program URL mappings into cache", urlProgramMap.size());
    }

    public Map<String, String> getUrlProgramMap() {
        return urlProgramMap;
    }
    
    @Transactional(readOnly = true)
    public List<Program> getAllPrograms() {
        return programRepository.findAll();
    }

    @Transactional
    public Program saveProgram(Program program) {
        Program saved = programRepository.save(program);
        refreshCache(); // 변경 시 캐시 갱신
        return saved;
    }

    @Transactional
    public void deleteProgram(java.util.UUID id) {
        programRepository.deleteById(id);
        refreshCache(); // 삭제 시 캐시 갱신
    }
}
