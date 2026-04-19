package com.example.ecommerce.security;

import com.example.ecommerce.service.ProgramService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final ProgramService programService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String requestPath = request.getRequestURI();
        String requestMethod = request.getMethod().toUpperCase();

        // 1. 요청된 URL에 매핑된 프로그램 코드 찾기
        Optional<ProgramService.ProgramCacheInfo> programInfo = findRequiredProgram(requestMethod, requestPath);

        // 2. 매핑된 프로그램 코드가 없다면 기본적으로 허용 (또는 정책에 따라 거부)
        // 여기서는 명시되지 않은 API는 인증된 사용자라면 허용하는 방식으로 설정
        if (programInfo.isEmpty()) {
            return new AuthorizationDecision(authentication.get() != null && authentication.get().isAuthenticated());
        }

        ProgramService.ProgramCacheInfo info = programInfo.get();

        // [추가] 공개 프로그램(isPublic = true)인 경우 인증 여부와 관계없이 허용
        if (info.isPublic()) {
            return new AuthorizationDecision(true);
        }
        
        // 3. 사용자가 해당 프로그램 권한을 가졌는지 확인
        // 익명 사용자는 authentication.get()이 null이거나 isAuthenticated()가 false일 수 있음
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        boolean hasPermission = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(info.getProgramCode()) || a.equals("ROLE_ADMIN") || a.equals("ROLE_DEVELOPER"));

        if (!hasPermission) {
            log.warn("Access Denied for user {} on {} {}. Required: {}", 
                auth.getName(), requestMethod, requestPath, info.getProgramCode());
        }

        return new AuthorizationDecision(hasPermission);
    }

    private Optional<ProgramService.ProgramCacheInfo> findRequiredProgram(String method, String path) {
        Map<String, ProgramService.ProgramCacheInfo> map = programService.getUrlProgramMap();
        
        // 정확히 일치하는 패턴 검색 (Method + Path)
        for (Map.Entry<String, ProgramService.ProgramCacheInfo> entry : map.entrySet()) {
            String[] parts = entry.getKey().split(" ");
            if (parts.length < 2) continue;
            
            String configMethod = parts[0];
            String configPath = parts[1];

            // 메소드가 ANY이거나 일치하고, 경로가 패턴에 맞으면 통과
            if ((configMethod.equals("ANY") || configMethod.equals(method)) 
                && pathMatcher.match(configPath, path)) {
                return Optional.of(entry.getValue());
            }
        }
        
        return Optional.empty();
    }
}
