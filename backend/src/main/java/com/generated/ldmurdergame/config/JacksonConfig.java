package com.generated.ldmurdergame.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 场次开始时间的 JSON 时间基准。
 *
 * <p>开始时间以门店墙钟存为 LocalDateTime（JVM 默认时区已由
 * {@link BusinessTimezonePostProcessor} 固定为门店时区）。对外统一序列化为
 * <b>带偏移的 ISO-8601</b>（如 {@code 2026-09-13T19:00:00+08:00}），代表一个
 * 确定的绝对时刻：任意浏览器时区解析出的 instant 都相同，页面不会提前/延迟截止。
 *
 * <p>入参支持三种：带偏移 / 带 Z 的绝对时刻（按门店时区换算墙钟），以及无时区墙钟串
 * （视为门店当地时间，兼容简单客户端）。
 *
 * <p>提供自定义 {@link JavaTimeModule} Bean 后，Spring Boot 默认的同名模块配置
 * （带 {@code @ConditionalOnMissingBean}）自动让位，不会重复注册。
 */
@Configuration
public class JacksonConfig {

  private static ZoneId businessZone() {
    return TimeZone.getDefault().toZoneId();
  }

  @Bean
  public JavaTimeModule businessJavaTimeModule() {
    JavaTimeModule module = new JavaTimeModule();
    module.addSerializer(LocalDateTime.class, new BusinessLocalDateTimeSerializer());
    module.addDeserializer(LocalDateTime.class, new BusinessLocalDateTimeDeserializer());
    return module;
  }

  /** LocalDateTime -> 带门店时区偏移的 ISO-8601 字符串（同一绝对时刻）。 */
  static class BusinessLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      ZoneOffset offset = businessZone().getRules().getOffset(value);
      gen.writeString(value.atOffset(offset).toString());
    }
  }

  /** 带偏移/Z 的绝对时刻按门店时区换算；无偏移墙钟串视为门店当地时间。 */
  static class BusinessLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      String text = parser.getValueAsString();
      if (text == null || text.isBlank()) {
        return null;
      }
      String trimmed = text.trim();
      // 含时区信息（偏移或 Z）：作为绝对时刻换算到门店时区墙钟
      if (hasZoneOrOffset(trimmed)) {
        return OffsetDateTime.parse(normalizeZ(trimmed))
            .atZoneSameInstant(businessZone())
            .toLocalDateTime();
      }
      return LocalDateTime.parse(trimmed);
    }

    private static boolean hasZoneOrOffset(String value) {
      return value.endsWith("Z") || value.endsWith("z")
          || value.matches(".*[+-]\\d{2}:\\d{2}(\\[.*])?$");
    }

    private static String normalizeZ(String value) {
      return value.endsWith("z") ? value.substring(0, value.length() - 1) + "Z" : value;
    }
  }
}
