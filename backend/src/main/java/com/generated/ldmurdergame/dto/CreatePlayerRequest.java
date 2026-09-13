package com.generated.ldmurdergame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 玩家注册入参（演示用，便于真实接口补充测试玩家） */
public record CreatePlayerRequest(
    @NotBlank(message = "玩家昵称不能为空")
    @Size(max = 80, message = "玩家昵称最长 80 个字符")
    String name,

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    String phone
) {
}
