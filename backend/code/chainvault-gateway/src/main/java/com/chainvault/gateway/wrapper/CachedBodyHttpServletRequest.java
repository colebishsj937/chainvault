package com.chainvault.gateway.wrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取请求体的 HttpServletRequest 包装器
 *
 * @author chainvault
 * @date 2026-06-05
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    /**
     * 缓存原始请求体
     *
     * @param request 原始请求
     * @throws IOException 读取失败
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.body = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /**
     * 获取缓存的请求体字节
     *
     * @return 请求体
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * 获取 UTF-8 请求体字符串
     *
     * @return 请求体文本
     */
    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
