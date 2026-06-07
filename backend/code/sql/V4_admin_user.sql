-- 运营后台用户表（JWT 登录）
-- 使用方式: docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < V4_admin_user.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS admin_user (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录用户名',
    password     VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
    display_name VARCHAR(64)  NOT NULL COMMENT '显示名称',
    role         VARCHAR(32)  NOT NULL DEFAULT 'ADMIN' COMMENT '角色：ADMIN/OPERATOR',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    last_login_at DATETIME    COMMENT '最近登录时间',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='运营后台用户表';

-- 默认账号: admin / admin123（生产环境请立即修改密码）
INSERT INTO admin_user (username, password, display_name, role, status) VALUES
('admin', '$2b$10$VaXLALGEMwOtQIzroQKazumOVmslZFSMh6q0AnCLcZZ3K1kvlc/kq', '系统管理员', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);
