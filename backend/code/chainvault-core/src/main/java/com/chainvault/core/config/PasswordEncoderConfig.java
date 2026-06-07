package com.chainvault.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置（Admin 登录等共用）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 密码编码器
     *
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
