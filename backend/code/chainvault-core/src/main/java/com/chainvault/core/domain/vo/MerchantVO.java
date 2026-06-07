package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.Merchant;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户信息视图（不含密钥）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class MerchantVO {

    private String merchantId;
    private String merchantName;
    private String apiKey;
    private String callbackUrl;
    private String ipWhitelist;
    private Integer status;
    private Integer tier;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     *
     * @param entity 商户实体
     * @return 视图对象
     */
    public static MerchantVO from(Merchant entity) {
        MerchantVO vo = new MerchantVO();
        vo.setMerchantId(entity.getMerchantId());
        vo.setMerchantName(entity.getMerchantName());
        vo.setApiKey(entity.getApiKey());
        vo.setCallbackUrl(entity.getCallbackUrl());
        vo.setIpWhitelist(entity.getIpWhitelist());
        vo.setStatus(entity.getStatus());
        vo.setTier(entity.getTier());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
