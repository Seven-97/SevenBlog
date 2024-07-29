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

 Date: 29/07/2024 23:13:47
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for award
-- ----------------------------
DROP TABLE IF EXISTS `award`;
CREATE TABLE `award`  (
  `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '自增的主键',
  `book_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图书号',
  `author_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者号',
  `cup_type` tinyint(4) NOT NULL COMMENT '奖项类型（0表示金；1表示银；2表示铜）',
  `cup_time` datetime NOT NULL COMMENT '获奖时间',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_state` tinyint(4) NOT NULL COMMENT '0表示正常使用；1表示已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `book_id`(`book_id`) USING BTREE COMMENT '唯一索引：图书号',
  INDEX `idx_author_id`(`author_id`) USING BTREE,
  INDEX `idx_author_time`(`author_id`, `cup_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '奖项表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
