-- V26: 상품목록 메뉴의 프로그램 연결 복구 및 경로 명시 
-- 1. 상품목록 메뉴에 PG_MKT_LIST 프로그램 다시 연결
UPDATE menus 
SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_MKT_LIST')
WHERE name = '상품목록';

-- 2. 상품목록 메뉴에 명시적인 경로('/') 부여 
-- 프로그램 연결과 별개로, 경로가 있어야 리프 노드로 인식되어 트리에서 사라지지 않음
UPDATE menus SET path = '/' WHERE name = '상품목록';

-- 3. (덤으로) 자재마켓 폴더 상단 배치 확인
UPDATE menus SET sort_order = 10 WHERE name = '자재마켓';
