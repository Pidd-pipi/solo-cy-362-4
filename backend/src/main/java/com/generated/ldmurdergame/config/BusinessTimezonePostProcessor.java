package com.generated.ldmurdergame.config;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 在所有 Bean 初始化之前，把 JVM 默认时区设为门店统一业务时区（默认东八区，
 * 可用 {@code APP_TIMEZONE} 覆盖）。
 *
 * <p>场次开始时间以“门店墙钟”存储为 LocalDateTime；将 JVM 默认时区固定为门店
 * 时区后，Jackson 会把它序列化为带同一偏移的 ISO-8601（绝对时刻），页面在任意
 * 浏览器时区解析到的都是同一个 instant，杜绝因浏览器时区不同导致的提前/延迟截止。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BusinessTimezonePostProcessor implements EnvironmentPostProcessor {

  public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment,
                                     SpringApplication application) {
    String zone = environment.getProperty("APP_TIMEZONE", DEFAULT_TIMEZONE);
    ZoneId zoneId;
    try {
      zoneId = ZoneId.of(zone);
    } catch (RuntimeException ex) {
      zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    }
    TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
  }
}
