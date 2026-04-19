-- V25: 프로그램에 공개 여부(is_public) 필드 추가 및 데이터 설정 
-- 1. programs 테이블에 is_public 컬럼 추가 (기본값 false)
ALTER TABLE programs ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. 상품 조회 관련 기능들을 '공개(is_public = true)'로 전환
-- 이를 통해 비회원(Guest)도 해당 기능을 사용할 수 있게 됨
UPDATE programs SET is_public = TRUE 
WHERE program_code IN ('PG_MKT_LIST', 'API_MKT_SEARCH');
