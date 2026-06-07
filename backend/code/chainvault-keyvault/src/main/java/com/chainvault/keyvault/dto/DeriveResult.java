package com.chainvault.keyvault.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地址派生结果
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeriveResult {

    /** 链上地址 */
    private String address;

    /** BIP44 派生路径 */
    private String bip44Path;
}
