-- 归集全局配置（单行，id 固定为 1）
CREATE TABLE IF NOT EXISTS sweep_config (
    id                    BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '固定为 1',
    sweep_enabled         TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用定时归集扫描：0=否 1=是',
    threshold_multiplier  INT             NOT NULL DEFAULT 5 COMMENT '归集阈值倍数，阈值=min_deposit×倍数',
    updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='归集全局配置表';

INSERT INTO sweep_config (id, sweep_enabled, threshold_multiplier)
VALUES (1, 1, 5)
ON DUPLICATE KEY UPDATE id = id;
