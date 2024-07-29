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

 Date: 29/07/2024 23:13:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for author
-- ----------------------------
DROP TABLE IF EXISTS `author`;
CREATE TABLE `author`  (
  `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '自增的主键',
  `author_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者号',
  `author_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者名',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者简介',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_state` tinyint(4) NOT NULL COMMENT '0表示正常使用；1表示已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `author_id`(`author_id`) USING BTREE COMMENT '唯一索引：作者号'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '作者表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
