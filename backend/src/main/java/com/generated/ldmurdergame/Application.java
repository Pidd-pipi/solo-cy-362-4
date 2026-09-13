package com.generated.ldmurdergame;

import com.generated.ldmurdergame.config.BusinessTimezonePostProcessor;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    // 兜底：与 BusinessTimezonePostProcessor 保持一致，保证开始时间按门店时区解释
    TimeZone.setDefault(TimeZone.getTimeZone(
        System.getenv().getOrDefault("APP_TIMEZONE", BusinessTimezonePostProcessor.DEFAULT_TIMEZONE)));
    SpringApplication.run(Application.class, args);
  }
}
