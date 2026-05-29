-- 秒杀系统数据库初始化

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE seckill;

-- 用户表
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id BIGINT NOT NULL COMMENT '用户ID(雪花算法)',
    nickname VARCHAR(32) NOT NULL COMMENT '昵称',
    password VARCHAR(128) NOT NULL COMMENT '密码(BCrypt)',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
DROP TABLE IF EXISTS t_goods;
CREATE TABLE t_goods (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    goods_name VARCHAR(64) NOT NULL COMMENT '商品名称',
    goods_title VARCHAR(128) DEFAULT NULL COMMENT '商品标题',
    goods_img VARCHAR(255) DEFAULT NULL COMMENT '商品图片URL',
    goods_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '原价',
    goods_stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    goods_detail TEXT COMMENT '商品详情',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 秒杀商品表
DROP TABLE IF EXISTS t_seckill_goods;
CREATE TABLE t_seckill_goods (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '秒杀商品ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    seckill_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '秒杀价',
    stock_count INT NOT NULL DEFAULT 0 COMMENT '秒杀库存',
    start_time DATETIME NOT NULL COMMENT '秒杀开始时间',
    end_time DATETIME NOT NULL COMMENT '秒杀结束时间',
    PRIMARY KEY (id),
    KEY idx_goods_id (goods_id),
    KEY idx_start_end (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- 订单表
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id BIGINT NOT NULL COMMENT '订单ID(雪花算法)',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    goods_name VARCHAR(64) DEFAULT NULL COMMENT '商品名称(冗余)',
    goods_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '成交价',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待支付 1:已支付 2:已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 秒杀订单表
DROP TABLE IF EXISTS t_seckill_order;
CREATE TABLE t_seckill_order (
    id BIGINT NOT NULL COMMENT '秒杀订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_uid_gid (user_id, goods_id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

-- 插入测试商品
INSERT INTO t_goods (id, goods_name, goods_title, goods_img, goods_price, goods_stock, goods_detail)
VALUES
(1, 'iPhone 15 Pro Max', '苹果旗舰手机', '/imgs/iphone15.png', 9999.00, 100, 'A17 Pro 芯片 / 钛金属设计'),
(2, 'MacBook Pro 14', '苹果笔记本', '/imgs/macbook14.png', 14999.00, 50, 'M3 Pro 芯片 / 18GB 内存'),
(3, 'AirPods Pro 2', '苹果降噪耳机', '/imgs/airpods.png', 1899.00, 200, 'H2 芯片 / USB-C 接口');

-- 插入秒杀商品 (时间设为未来几天)
INSERT INTO t_seckill_goods (goods_id, seckill_price, stock_count, start_time, end_time)
VALUES
(1, 5999.00, 20, '2026-05-29 10:00:00', '2026-05-30 10:00:00'),
(2, 9999.00, 10, '2026-05-29 12:00:00', '2026-05-30 12:00:00'),
(3, 999.00,  50, '2026-05-29 14:00:00', '2026-05-30 14:00:00');
