package com.chainvault.keyvault.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.constants.ChainCode;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.keyvault.config.KeyVaultProperties;
import com.chainvault.keyvault.domain.entity.MasterKey;
import com.chainvault.keyvault.dto.DeriveResult;
import com.chainvault.keyvault.mapper.MasterKeyMapper;
import com.chainvault.keyvault.service.AddressValidator;
import com.chainvault.keyvault.service.KeyVaultService;
import com.chainvault.keyvault.util.AesUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDPath;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

/**
 * 密钥管理业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeyVaultServiceImpl implements KeyVaultService {

    private static final String DEFAULT_KEY_ID = "default";

    private final MasterKeyMapper masterKeyMapper;
    private final KeyVaultProperties keyVaultProperties;
    private final AddressValidator addressValidator;

    /** HD 根密钥链，仅驻留内存 */
    private DeterministicKeyChain keyChain;

    /**
     * 启动时加载或初始化主助记词
     */
    @PostConstruct
    public void init() {
        // 1. 从数据库加载加密助记词
        String mnemonic = loadOrCreateMnemonic();

        // 2. 从助记词恢复 HD 密钥链
        try {
            List<String> wordList = List.of(mnemonic.trim().split("\\s+"));
            DeterministicSeed seed = new DeterministicSeed(wordList, null, "", 0L);
            keyChain = DeterministicKeyChain.builder().seed(seed).build();
            log.info("KeyVault 主密钥加载成功");
        } catch (Exception e) {
            throw new IllegalStateException("KeyVault 主密钥初始化失败", e);
        }
    }

    /**
     * 派生充值地址
     *
     * @param chainCode    链标识
     * @param accountIndex 商户 account 索引
     * @param addressIndex 地址索引
     * @return 派生结果
     */
    @Override
    public DeriveResult deriveAddress(String chainCode, int accountIndex, int addressIndex) {
        // 1. 构建 BIP44 路径并派生子密钥
        HDPath path = buildHdPath(chainCode, accountIndex, addressIndex);
        String pathStr = path.toString();
        DeterministicKey childKey = keyChain.getKeyByPath(path, true);

        // 2. 按链类型生成地址
        String address = switch (chainCode.toUpperCase()) {
            case ChainCode.ETH, ChainCode.BNB -> deriveEthAddress(childKey);
            case ChainCode.BTC -> deriveBtcAddress(childKey);
            case ChainCode.TRON -> deriveTronAddress(childKey);
            default -> throw new BusinessException("不支持的链: " + chainCode);
        };

        log.info("派生地址 chain={} path={} address={}", chainCode, pathStr, address);
        return new DeriveResult(address, pathStr);
    }

    /**
     * 构建 BIP44 路径
     *
     * @param chainCode    链标识
     * @param accountIndex 商户 account 索引
     * @param addressIndex 地址索引
     * @return 路径字符串
     */
    @Override
    public String buildBip44Path(String chainCode, int accountIndex, int addressIndex) {
        return buildHdPath(chainCode, accountIndex, addressIndex).toString();
    }

    // 构建 BIP44 HD 路径对象
    private HDPath buildHdPath(String chainCode, int accountIndex, int addressIndex) {
        int coinType = ChainCode.bip44CoinType(chainCode);
        return HDPath.M(
                new ChildNumber(44, true),
                new ChildNumber(coinType, true),
                new ChildNumber(accountIndex, true),
                ChildNumber.ZERO,
                new ChildNumber(addressIndex, false)
        );
    }

    /**
     * 对交易数据签名
     *
     * @param bip44Path BIP44 路径
     * @param txData    待签名数据
     * @return DER 签名
     */
    @Override
    public byte[] sign(String bip44Path, byte[] txData) {
        // 1. 派生签名密钥
        DeterministicKey signingKey = keyChain.getKeyByPath(parseStoredPath(bip44Path), true);
        ECKey ecKey = ECKey.fromPrivate(signingKey.getPrivKey());

        // 2. ECDSA 签名
        ECKey.ECDSASignature signature = ecKey.sign(Sha256Hash.wrap(txData));
        return signature.encodeToDER();
    }

    /**
     * 校验地址格式
     *
     * @param chainCode 链标识
     * @param address   地址
     * @return true=合法
     */
    @Override
    public boolean validateAddress(String chainCode, String address) {
        return addressValidator.isValid(chainCode, address);
    }

    /**
     * 加载或创建助记词
     *
     * @return 明文助记词
     */
    protected String loadOrCreateMnemonic() {
        MasterKey record = masterKeyMapper.selectOne(
                new LambdaQueryWrapper<MasterKey>().eq(MasterKey::getKeyId, DEFAULT_KEY_ID));

        if (record != null) {
            return AesUtil.decrypt(record.getEncryptedMnemonic(), keyVaultProperties.getEncryptKey());
        }

        // 开发环境：自动生成并落库
        String mnemonic = generateMnemonic();
        MasterKey created = new MasterKey();
        created.setKeyId(DEFAULT_KEY_ID);
        created.setEncryptedMnemonic(AesUtil.encrypt(mnemonic, keyVaultProperties.getEncryptKey()));
        masterKeyMapper.insert(created);
        log.warn("已自动生成开发用主助记词并写入 master_key 表，生产环境请替换为安全助记词");
        return mnemonic;
    }

    // 解析已存储的 BIP44 路径字符串
    private HDPath parseStoredPath(String bip44Path) {
        String[] segments = bip44Path.replace("m/", "").split("/");
        java.util.List<ChildNumber> children = new java.util.ArrayList<>();
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            boolean hardened = segment.endsWith("'");
            int index = Integer.parseInt(hardened ? segment.substring(0, segment.length() - 1) : segment);
            children.add(new ChildNumber(index, hardened));
        }
        return HDPath.M(children);
    }

    // 生成 12 词助记词
    private String generateMnemonic() {
        try {
            byte[] entropy = new byte[16];
            new SecureRandom().nextBytes(entropy);
            return String.join(" ", MnemonicCode.INSTANCE.toMnemonic(entropy));
        } catch (Exception e) {
            throw new BusinessException("助记词生成失败");
        }
    }

    // ETH / BNB 地址派生
    private String deriveEthAddress(DeterministicKey childKey) {
        BigInteger privateKey = new BigInteger(1, childKey.getPrivKeyBytes());
        return Numeric.prependHexPrefix(Keys.getAddress(privateKey));
    }

    // BTC P2SH-P2WPKH 地址派生（3 开头）
    private String deriveBtcAddress(DeterministicKey childKey) {
        byte[] pubKey = childKey.getPubKey();
        byte[] pubKeyHash = Utils.sha256hash160(pubKey);
        Script redeemScript = ScriptBuilder.createP2WPKHOutputScript(pubKeyHash);
        Script p2sh = ScriptBuilder.createP2SHOutputScript(redeemScript);
        return p2sh.getToAddress(MainNetParams.get()).toString();
    }

    // TRON 地址派生（0x41 前缀 + Base58Check）
    private String deriveTronAddress(DeterministicKey childKey) {
        BigInteger privateKey = new BigInteger(1, childKey.getPrivKeyBytes());
        byte[] ethAddr = Numeric.hexStringToByteArray(Keys.getAddress(privateKey));
        return Base58.encodeChecked(0x41, ethAddr);
    }
}
