package com.chainvault.keyvault.service;

import com.chainvault.keyvault.dto.DeriveResult;

/**
 * 密钥管理业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface KeyVaultService {

    /**
     * 按 BIP44 路径派生充值地址
     *
     * @param chainCode    链标识
     * @param accountIndex 商户 account 索引
     * @param addressIndex 地址索引
     * @return 派生结果（地址 + 路径）
     */
    DeriveResult deriveAddress(String chainCode, int accountIndex, int addressIndex);

    /**
     * 构建 BIP44 路径字符串
     *
     * @param chainCode    链标识
     * @param accountIndex 商户 account 索引
     * @param addressIndex 地址索引
     * @return BIP44 路径
     */
    String buildBip44Path(String chainCode, int accountIndex, int addressIndex);

    /**
     * 用指定 BIP44 路径对交易数据签名
     *
     * @param bip44Path BIP44 路径
     * @param txData    待签名原始字节
     * @return DER 编码签名
     */
    byte[] sign(String bip44Path, byte[] txData);

    /**
     * 校验地址格式
     *
     * @param chainCode 链标识
     * @param address   地址
     * @return true=合法
     */
    boolean validateAddress(String chainCode, String address);
}
