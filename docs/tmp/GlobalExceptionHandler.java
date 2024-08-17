package com.qunar.corp.qboss.exam.bank.exception;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.exception.ExcelAnalysisStopException;
import com.qunar.corp.qboss.exam.bank.result.Result;
import com.qunar.corp.qboss.exam.bank.result.enums.TaskResultConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

import static com.qunar.corp.qboss.exam.bank.result.enums.GlobalExceptionResultConstants.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = TaskException.class)
    public Result handlerTaskException(TaskException e, HttpServletRequest request) {
        String message = e.getMessage();
        log.error("请求[ {} ] {} 的校验发生错误，错误信息：{}", request.getMethod(), request.getRequestURL(), message, e);
        return Result.error(TaskResultConstants.IMPORT_FAIL.getCode(), message);
    }

    @ExceptionHandler(value = ExcelValidatorException.class)
    public Result handlerEasyExcelValidatorException(ExcelValidatorException e, HttpServletRequest request) {
        String message = e.getMessage();
        log.error("请求[ {} ] {} 的校验发生错误，错误信息：{}", request.getMethod(), request.getRequestURL(), message, e);
        return Result.error(TaskResultConstants.IMPORT_FAIL.getCode(), message);
    }

    @ExceptionHandler(value = BindException.class)
    public Result handlerBindException(BindException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        String validExceptionMsg = getValidExceptionMsg(bindingResult.getAllErrors());
        log.error("请求[ {} ] {} 的参数校验发生错误，错误信息：{}", request.getMethod(), request.getRequestURL(), validExceptionMsg, e);
        return Result.error(ERROR_BIND_PARAM.getCode(), validExceptionMsg);
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    public Result handlerConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> sets = e.getConstraintViolations();
        StringBuilder sb = new StringBuilder();
        if (CollUtil.isNotEmpty(sets)) {
            sets.forEach(error -> {
                sb.append(error.getPropertyPath().toString()).append(":").append(error.getMessage()).append(";");
            });
        }
        String msg = sb.toString();
        log.error("请求[ {} ] {} 的参数校验发生错误，错误信息：{}", request.getMethod(), request.getRequestURL(), msg, e);
        return Result.error(ERROR_VIOLATION_PARAM.getCode(), msg);
    }

    @ExceptionHandler(value = Exception.class)
    public Result handlerException(Exception e, HttpServletRequest request) {
        log.error("请求[ {} ] {} 发生未知异常", request.getMethod(), request.getRequestURL(), e);
        return Result.error(ERROR_UNKNOWN);
    }

    private String getValidExceptionMsg(List<ObjectError> errors) {
        if (CollUtil.isNotEmpty(errors)) {
            StringBuilder sb = new StringBuilder();
            errors.forEach(error -> {
                if (error instanceof FieldError fieldError) {
                    sb.append(fieldError.getField()).append(":");
                }
                sb.append(error.getDefaultMessage()).append(";");
            });
            return sb.toString();
        }
        return null;
    }
}
