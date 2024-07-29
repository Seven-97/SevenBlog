/*
 Navicat Premium Data Transfer

 Source Server         : mysql5
 Source Server Type    : MySQL
 Source Server Version : 50739
 Source Host           : localhost:3307
 Source Schema         : qfc_mysql

 Target Server Type    : MySQL
 Target Server Version : 50739
 File Encoding         : 65001

 Date: 29/07/2024 23:01:01
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for book
-- ----------------------------
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book`  (
  `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '自增的主键',
  `book_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图书号',
  `author_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者号',
  `book_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '书名',
  `pages` int(11) NOT NULL COMMENT '页数',
  `price` decimal(10, 2) NOT NULL COMMENT '价格',
  `press` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '出版社',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_state` tinyint(4) NOT NULL COMMENT '0表示正常使用；1表示已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `book_id`(`book_id`) USING BTREE COMMENT '唯一索引：图书号'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '图书表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
