/**
 * programs.ts — WEB 타입 프로그램 목록을 DB에서 로드하여
 * 현재 경로에 필요한 programCode를 반환하는 스토어.
 *
 * BE의 DynamicAuthorizationManager가 DB의 URL 패턴을 읽어
 * API 접근을 제어하는 것과 동일한 방식으로,
 * FE 라우터 가드도 이 스토어를 통해 DB 기반으로 동적 접근 제어를 수행한다.
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../utils/api'

interface WebProgram {
  id: string
  programCode: string
  name: string
  url: string
  type: string
}

export const useProgramStore = defineStore('programs', () => {
  const webPrograms = ref<WebProgram[]>([])
  const loaded = ref(false)

  const fetchWebPrograms = async () => {
    try {
      const res = await api.get('/api/admin/programs')
      // WEB 타입만 필터링: 라우트 가드용
      webPrograms.value = (res.data as WebProgram[]).filter(p => p.type === 'WEB')
      loaded.value = true
    } catch (e) {
      // 로드 실패 시 빈 배열 유지 → 권한 없는 경로는 기본 차단되지 않음
      // (BE DynamicAuthorizationManager의 "매핑 없으면 인증 유저 허용" 정책과 동일)
      console.warn('[programs store] WEB 프로그램 목록 로드 실패, 라우트 가드 비활성화됨.', e)
      loaded.value = true
    }
  }

  /**
   * 현재 경로(path)에 필요한 programCode를 DB 기반으로 찾아 반환한다.
   * 매핑되는 프로그램이 없으면 null (→ 로그인 여부만 체크).
   *
   * 패턴 우선순위: 정확 일치 > 경로 깊이 기반 (더 구체적인 패턴이 우선).
   * BE AntPathMatcher의 동작을 단순화하여 구현:
   *   /admin/**          → /admin/로 시작하는 모든 경로
   *   /admin/orders      → 정확히 /admin/orders
   */
  const getRequiredPermission = (path: string): string | null => {
    let bestMatch: { programCode: string; specificity: number } | null = null

    for (const program of webPrograms.value) {
      // DB URL 앞에 /가 없으면 붙여서 정규화
      const pattern = program.url.startsWith('/') ? program.url : '/' + program.url
      const specificity = calcSpecificity(pattern)

      if (matchesAntPath(pattern, path)) {
        if (!bestMatch || specificity > bestMatch.specificity) {
          bestMatch = { programCode: program.programCode, specificity }
        }
      }
    }

    return bestMatch?.programCode ?? null
  }

  return { webPrograms, loaded, fetchWebPrograms, getRequiredPermission }
})

// ─── 내부 유틸 ──────────────────────────────────────────────────────────────

/**
 * AntPath 패턴 매칭 (BE AntPathMatcher 단순화 구현)
 * 지원 패턴:
 *   /exact         → 정확 일치
 *   /prefix/**     → 해당 경로 이하 모든 경로
 *   /prefix/*      → 바로 아래 단계 경로 하나
 */
function matchesAntPath(pattern: string, path: string): boolean {
  if (pattern === path) return true

  if (pattern.endsWith('/**')) {
    const prefix = pattern.slice(0, -3) // '/**' 제거
    return path === prefix || path.startsWith(prefix + '/')
  }

  if (pattern.endsWith('/*')) {
    const prefix = pattern.slice(0, -2) // '/*' 제거
    if (!path.startsWith(prefix + '/')) return false
    const rest = path.slice(prefix.length + 1)
    return !rest.includes('/') // 하위 경로 없이 한 단계만
  }

  return false
}

/**
 * 패턴의 구체성 점수 (높을수록 더 구체적 → 우선순위 높음)
 * 정확 일치 > /* > /** 순서
 */
function calcSpecificity(pattern: string): number {
  if (!pattern.includes('*')) return 1000                        // 정확 일치
  if (pattern.endsWith('/*')) return pattern.split('/').length * 10  // 단일 와일드카드
  if (pattern.endsWith('/**')) return pattern.split('/').length      // 다중 와일드카드
  return 1
}
