package com.chainvault.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ChainVault 网关启动类
 *
 * @author chainvault
 * @date 2026-06-05
 */
@SpringBootApplication(scanBasePackages = "com.chainvault")
@EnableScheduling
public class GatewayApplication {

    /**
     * 应用入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
