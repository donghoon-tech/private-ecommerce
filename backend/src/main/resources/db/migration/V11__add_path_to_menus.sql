-- 메뉴마다 고유한 UI 라우팅 경로가 있어야 하므로 추가합니다.
ALTER TABLE menus ADD COLUMN path VARCHAR(255);

-- 기존 데이터에 path 채워넣기
UPDATE menus SET path = '/admin/roles' WHERE name = '권한 마스터 관리';
UPDATE menus SET path = '/admin/menus' WHERE name = '프로그램 등록관리';
UPDATE menus SET path = '/' WHERE name = '자재 마켓';
UPDATE menus SET path = '/admin/orders' WHERE name = '주문/배차 내역';
UPDATE menus SET path = '/admin/dash' WHERE name = '대시보드';

-- 폴더들은 # 으로 처리
UPDATE menus SET path = '#' WHERE path IS NULL;
