package com.ctdi.llmtc.xtp.traininfer.advise;

import com.ctdi.llmtc.xtp.traininfer.util.OperateResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ValidationException;
import java.util.stream.Collectors;


/**
 * <h1>全局异常处理</h1>
 *
 *
 */
@RestControllerAdvice("com.ctdi.llmtc.xtp")
@Slf4j
public class GlobalExceptionAdvice {

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public OperateResult<String> handleException(Exception ex, HttpServletRequest request){
        log.error("Unhandled exception from URL [" + request.getRequestURI() + "]", ex);
        return OperateResult.error(ex.getMessage());
    }

    /**
     * 自定义运行时异常。
     *
     * @param ex       异常对象。
     * @param request  http请求。
     * @return 应答对象。
     */
    @ExceptionHandler(value = RuntimeException.class)
    public OperateResult<String> runtimeExceptionHandle(Exception ex, HttpServletRequest request) {
        log.error("runtimeException exception from URL [" + request.getRequestURI() + "]", ex);
        return OperateResult.error(ex.getMessage());
    }

    /**
     * 处理方法参数无效异常。
     *
     * @param ex       异常对象。
     * @param request  http请求。
     * @return 应答对象。
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public OperateResult<String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("MethodArgumentNotValidException exception from URL [" + request.getRequestURI() + "]", ex);
        String errorMessages = ex.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(";"));
        return OperateResult.error(errorMessages);
    }

    /**
     * 处理数据验证失败异常。
     *
     * @param ex       异常对象。
     * @param request  http请求。
     * @return 应答对象。
     */
    @ExceptionHandler(value = ValidationException.class)
    public OperateResult<String> handleMethodArgumentNotValid(ValidationException ex, HttpServletRequest request) {
        log.error("DataValidationException exception from URL [" + request.getRequestURI() + "]", ex);
        return OperateResult.error(ex.getMessage());
    }
}
