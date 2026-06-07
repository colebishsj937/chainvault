package com.chainvault.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Webhook 投递用 RestTemplate
     *
     * @param properties 配置
     * @return RestTemplate
     */
    @Bean
    public RestTemplate webhookRestTemplate(CoreProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getWebhookTimeoutMs());
        factory.setReadTimeout(properties.getWebhookTimeoutMs());
        return new RestTemplate(factory);
    }
}
