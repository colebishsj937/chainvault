package com.chainvault.gateway.filter;

import com.chainvault.common.constants.SignConstants;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.result.ApiResult;
import com.chainvault.common.redis.SlidingWindowRateLimiter;
import com.chainvault.common.util.SignUtil;
import com.chainvault.core.domain.entity.Merchant;
import com.chainvault.core.service.MerchantService;
import com.chainvault.gateway.config.GatewayProperties;
import com.chainvault.gateway.wrapper.CachedBodyHttpServletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * MD5 签名校验过滤器
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class SignatureFilter extends OncePerRequestFilter {

    private final GatewayProperties gatewayProperties;
    private final MerchantService merchantService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SlidingWindowRateLimiter rateLimiter;

    /**
     * 校验 MD5 签名并防重放
     *
     * @param request     请求
     * @param response    响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 跳过免签名路径
        if (!gatewayProperties.isSignEnabled() || isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 读取签名头
        String apiKey = request.getHeader(SignConstants.HEADER_API_KEY);
        String timestamp = request.getHeader(SignConstants.HEADER_TIMESTAMP);
        String nonce = request.getHeader(SignConstants.HEADER_NONCE);
        String sign = request.getHeader(SignConstants.HEADER_SIGN);

        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce) || !StringUtils.hasText(sign)) {
            writeError(response, 401, "缺少签名头");
            return;
        }

        // 3. 滑动窗口限流（按 apiKey）
        if (gatewayProperties.isRateLimitEnabled()) {
            boolean allowed = rateLimiter.isAllowed(
                    apiKey,
                    gatewayProperties.getRateLimitWindowSeconds(),
                    gatewayProperties.getRateLimitMaxRequests(),
                    nonce
            );
            if (!allowed) {
                writeError(response, 429, "请求过于频繁");
                return;
            }
        }

        // 4. 时间戳校验（±5 分钟）
        long requestTs;
        try {
            requestTs = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            writeError(response, 401, "时间戳格式错误");
            return;
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - requestTs) > SignConstants.TIMESTAMP_TOLERANCE_SECONDS) {
            writeError(response, 401, "请求已过期");
            return;
        }

        // 5. nonce 防重放
        String nonceKey = SignConstants.NONCE_KEY_PREFIX + apiKey + ":" + nonce;
        Boolean isNew = redis.opsForValue().setIfAbsent(
                nonceKey, "1", SignConstants.NONCE_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            writeError(response, 401, "重复请求");
            return;
        }

        // 6. 缓存请求体
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String body = cachedRequest.getBodyAsString();

        // 7. 查询商户密钥
        String secretKey;
        String merchantId;
        try {
            Merchant merchant = merchantService.getByApiKey(apiKey);
            secretKey = merchant.getSecretKey();
            merchantId = merchant.getMerchantId();
        } catch (BusinessException e) {
            writeError(response, e.getCode(), e.getMessage());
            return;
        }

        // 8. 比对 MD5 签名
        String expected = SignUtil.sign(body, timestamp, nonce, secretKey);
        if (!expected.equalsIgnoreCase(sign)) {
            log.warn("签名失败 apiKey={} uri={}", apiKey, request.getRequestURI());
            writeError(response, 401, "签名验证失败");
            return;
        }

        // 9. 写入商户上下文
        cachedRequest.setAttribute(SignConstants.ATTR_MERCHANT_ID, merchantId);
        filterChain.doFilter(cachedRequest, response);
    }

    // 判断是否免签名路径
    private boolean isPublicPath(String uri) {
        for (String prefix : gatewayProperties.getPublicPaths()) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // 输出 JSON 错误
    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(code, message)));
    }
}
