-- 1. programs 테이블에 http_method 컬럼 추가 (API 권한 식별용)
ALTER TABLE programs ADD COLUMN http_method VARCHAR(10);

-- 2. 기존 API 데이터에 HTTP 메소드 업데이트
UPDATE programs SET http_method = 'GET' WHERE program_code IN ('API_DASH_STATS', 'API_MKT_SEARCH', 'API_ORDER_DETAIL');
UPDATE programs SET http_method = 'POST' WHERE program_code IN ('API_AUTH_SYNC');

-- 3. WEB 타입의 경우 메소드가 큰 의미는 없지만 일단 ALL 또는 GET으로 세팅
UPDATE programs SET http_method = 'GET' WHERE type = 'WEB';

-- 4. 향후 신규 데이터 추가를 위해 NOT NULL 제약조건 추가 (기존 데이터 업데이트 후)
-- ALTER TABLE programs ALTER COLUMN http_method SET NOT NULL; -- 필요시 나중에 추가
