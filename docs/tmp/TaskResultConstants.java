package com.qunar.corp.qboss.exam.bank.result.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.qunar.corp.qboss.exam.bank.result.IResultConstants;
import lombok.Getter;

/**
 * @author chenghaoy.yu
 * @description 任务 结果常量
 * @email chenghaoy.yu@qunar.com
 * @Date 2024/8/12 20:25
 */
@Getter
public enum TaskResultConstants implements IResultConstants {
    IMPORT_SUCCESS("0000", "任务导入成功"),
    DELETE_SUCCESS("0000", "删除任务成功"),

    IMPORT_FAIL("T1001", "任务导入失败"),
    TASK_QUERY_FAIL("T1002", "根据您提供的条件查询无任务"),
    DELETE_FAIL("T1003", "删除任务失败"),
    ;

    @EnumValue
    private final String code;

    @JsonValue
    private final String message;

    TaskResultConstants(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
