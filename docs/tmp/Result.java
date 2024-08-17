package com.qunar.corp.qboss.exam.bank.result;

import lombok.Data;

@Data
public class Result {

    private String code;
    private String message;
    private Object data;

    public static final String SUCCESSCODE = "0000";
    public static final String SUCCESSMSG = "成功";
    public static final String ERROR_CODE = "1111";
    public static final String ERROR_MSG = "失败";

    private Result() {
        this(SUCCESSCODE, SUCCESSMSG, null);
    }

    private Result(Object data) {
        this(SUCCESSCODE, SUCCESSMSG, data);
    }

    private Result(String code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    private Result(String code, String message) {
        this(code, message, null);
    }

    public static Result success() {
        return new Result();
    }

    public static Result success(Object data) {
        return new Result(data);
    }

    public static Result success(String code, String message) {
        return new Result(code, message);
    }

    public static Result success(String code, String message, Object data) {
        return new Result(code, message, data);
    }

    public static Result success(IResultConstants resultConstants) {
        return success(resultConstants.getCode(), resultConstants.getMessage());
    }

    public static Result success(IResultConstants resultConstants, Object data) {
        return success(resultConstants.getCode(), resultConstants.getMessage(),data);
    }

    public static Result error(String retCode, String retMsg) {
        return new Result(retCode, retMsg);
    }

    public static Result error(IResultConstants resultConstants) {
        return error(resultConstants.getCode(), resultConstants.getMessage());
    }
}
