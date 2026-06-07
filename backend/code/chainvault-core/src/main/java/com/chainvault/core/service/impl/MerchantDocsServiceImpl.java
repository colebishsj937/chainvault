package com.chainvault.core.service.impl;

import com.chainvault.common.exception.BusinessException;
import com.chainvault.core.domain.vo.MerchantDocsVO;
import com.chainvault.core.service.MerchantDocsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 商户 API 文档服务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Slf4j
@Service
public class MerchantDocsServiceImpl implements MerchantDocsService {

    private static final Pattern VERSION_PATTERN = Pattern.compile("版本：([^·]+)");
    private static final Pattern DATE_PATTERN = Pattern.compile("更新日期：(\\d{4}-\\d{2}-\\d{2})");

    private final ResourceLoader resourceLoader;

    /** Gateway Base URL，用于文档页展示 */
    @Value("${chainvault.docs.gateway-base-url:http://localhost:8082}")
    private String gatewayBaseUrl;

    public MerchantDocsServiceImpl(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 读取商户 API 文档 Markdown
     *
     * @return 文档内容与元信息
     */
    @Override
    public MerchantDocsVO getMerchantDocs() {
        // 1. 从 classpath 加载文档
        Resource resource = resourceLoader.getResource("classpath:docs/MERCHANT_API.md");
        if (!resource.exists()) {
            throw new BusinessException("商户 API 文档不存在");
        }

        String markdown;
        try {
            markdown = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("读取商户 API 文档失败", ex);
            throw new BusinessException("读取商户 API 文档失败");
        }

        // 2. 解析元信息
        MerchantDocsVO vo = new MerchantDocsVO();
        vo.setTitle("ChainVault 商户 API 对接文档");
        vo.setVersion(extract(VERSION_PATTERN, markdown, "1.0.0-SNAPSHOT"));
        vo.setUpdatedAt(extract(DATE_PATTERN, markdown, ""));
        vo.setGatewayBaseUrl(gatewayBaseUrl);
        vo.setMarkdown(markdown);
        return vo;
    }

    // 从 Markdown 首段提取元信息
    private String extract(Pattern pattern, String text, String defaultValue) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }
}
