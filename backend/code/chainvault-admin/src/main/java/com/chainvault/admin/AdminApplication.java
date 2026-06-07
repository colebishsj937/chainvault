package com.chainvault.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ChainVault 运营后台启动类
 *
 * @author chainvault
 * @date 2026-06-05
 */
@SpringBootApplication(scanBasePackages = "com.chainvault")
public class AdminApplication {

    /**
     * 应用入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
