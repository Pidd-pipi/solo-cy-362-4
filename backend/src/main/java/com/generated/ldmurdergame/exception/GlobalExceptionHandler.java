package com.generated.ldmurdergame.exception;

import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 业务规则错误：重复报名、非法取消、场次状态不允许等，均给出明确中文提示。 */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, String>> handleApiException(ApiException exception) {
    return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
  }

  /** 并发抢位时若唯一约束兜底命中（同一玩家重复有效报名），返回冲突提示。 */
  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<Map<String, String>> handleDuplicateKey(DuplicateKeyException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("message", "操作过于频繁或已存在有效报名，请勿重复提交"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleInvalidBody(
      MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getDefaultMessage())
        .orElse("请求参数校验失败");
    return ResponseEntity.badRequest().body(Map.of("message", message));
  }

  @ExceptionHandler({HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
  public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
    return ResponseEntity.badRequest().body(Map.of("message", "请求参数格式不正确"));
  }
}
