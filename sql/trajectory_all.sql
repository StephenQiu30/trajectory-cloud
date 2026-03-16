-- ============================================
-- 轨迹-基于AIGC的数据可视化平台 统一数据库脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS `trajectory` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `trajectory`;

-- ============================================
-- 用户表
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `user_name`       varchar(256)          DEFAULT NULL COMMENT '用户昵称',
    `user_avatar`     varchar(1024)         DEFAULT NULL COMMENT '用户头像',
    `user_profile`    varchar(512)          DEFAULT NULL COMMENT '用户简介',
    `user_role`       varchar(256) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin/ban',
    `user_email`      varchar(256)          DEFAULT NULL COMMENT '用户邮箱',
    `email_verified`  tinyint               DEFAULT 0 COMMENT '邮箱是否验证：0-未验证，1-已验证',
    `user_phone`      varchar(128)          DEFAULT NULL COMMENT '用户手机号',
    `mp_open_id`      varchar(256)          DEFAULT NULL COMMENT '微信公众号 OpenID',
    `wx_union_id`     varchar(256)          DEFAULT NULL COMMENT '微信 UnionID',
    `wx_open_id`      varchar(256)          DEFAULT NULL COMMENT '微信开放平台 OpenID',
    `github_id`       varchar(256)          DEFAULT NULL COMMENT 'GitHub ID',
    `github_login`    varchar(256)          DEFAULT NULL COMMENT 'GitHub 账号',
    `github_url`      varchar(512)          DEFAULT NULL COMMENT 'GitHub 主页',
    `last_login_time` datetime              DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   varchar(128)          DEFAULT NULL COMMENT '最后登录IP',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`       tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`user_email`) COMMENT '用户邮箱唯一索引',
    KEY `idx_github_id` (`github_id`) COMMENT 'GitHub ID索引',
    KEY `idx_wx_union_id` (`wx_union_id`) COMMENT '微信 UnionID索引',
    KEY `idx_user_phone` (`user_phone`) COMMENT '用户手机号索引',
    KEY `idx_user_email_is_delete` (`user_email`, `is_delete`) COMMENT '用户邮箱删除状态索引',
    KEY `idx_github_id_is_delete` (`github_id`, `is_delete`) COMMENT 'GitHub ID删除状态索引',
    KEY `idx_wx_union_id_is_delete` (`wx_union_id`, `is_delete`) COMMENT '微信 UnionID删除状态索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- ============================================
-- 通知表
-- ============================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `title`        varchar(256) NOT NULL COMMENT '通知标题',
    `content`      text         NOT NULL COMMENT '通知内容',
    `type`         varchar(64)  NOT NULL COMMENT '通知类型（system-系统通知，user-用户通知，analysis-分析通知，broadcast-全员广播）',
    `biz_id`       varchar(128) NOT NULL DEFAULT '' COMMENT '业务幂等ID',
    `user_id`      bigint       NOT NULL COMMENT '接收用户ID',
    `related_id`   bigint                DEFAULT NULL COMMENT '关联对象ID',
    `related_type` varchar(64)  NOT NULL DEFAULT '' COMMENT '关联对象类型',
    `is_read`      tinyint      NOT NULL DEFAULT 0 COMMENT '是否已读',
    `status`       tinyint      NOT NULL DEFAULT 0 COMMENT '状态（0-正常，1-停用）',
    `content_url`  varchar(512) NOT NULL DEFAULT '' COMMENT '跳转链接',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '用户ID索引',
    UNIQUE KEY `uk_biz_user` (`biz_id`, `user_id`) COMMENT '业务幂等去重',
    KEY `idx_type` (`type`) COMMENT '通知类型索引',
    KEY `idx_is_read` (`is_read`) COMMENT '已读状态索引',
    KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引',
    KEY `idx_user_id_is_read_create_time` (`user_id`, `is_read`, `create_time` DESC) COMMENT '用户未读通知按时间倒序索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '通知表';

-- ============================================
-- 图表信息表
-- ============================================
DROP TABLE IF EXISTS `chart`;
CREATE TABLE `chart`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '图表ID',
    `goal`         text                  DEFAULT NULL COMMENT '分析目标',
    `name`         varchar(128)          DEFAULT NULL COMMENT '图表名称',
    `chart_data`   LONGTEXT              DEFAULT NULL COMMENT '原始 CSV 数据',
    `chart_type`   varchar(128)          DEFAULT NULL COMMENT '图表类型',
    `gen_chart`    LONGTEXT              DEFAULT NULL COMMENT '生成的图表配置（JSON）',
    `gen_result`   LONGTEXT              DEFAULT NULL COMMENT '生成的分析结论',
    `status`       varchar(128) NOT NULL DEFAULT 'wait' COMMENT '状态 (wait, running, succeed, failed)',
    `exec_message` LONGTEXT              DEFAULT NULL COMMENT '执行详情/错误信息',
    `user_id`      bigint                DEFAULT NULL COMMENT '创建用户ID',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '图表信息表';
