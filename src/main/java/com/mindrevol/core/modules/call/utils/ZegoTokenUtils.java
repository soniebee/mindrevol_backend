package com.mindrevol.core.modules.call.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

public class ZegoTokenUtils {
    public static String generateToken04(long appId, String userId, String secret, int effectiveTimeInSeconds) {
        try {
            long createTime = System.currentTimeMillis() / 1000;
            long expireTime = createTime + effectiveTimeInSeconds;
            long nonce = new Random().nextInt(Integer.MAX_VALUE);

            // 1. Tạo JSON Payload
            String payload = String.format("{\"app_id\":%d,\"user_id\":\"%s\",\"nonce\":%d,\"ctime\":%d,\"expire\":%d,\"payload\":\"\"}",
                    appId, userId, nonce, createTime, expireTime);

            // 2. Mã hóa AES/CBC/PKCS5Padding
            String ivStr = String.format("%016d", Math.abs(new Random().nextLong())).substring(0, 16);
            byte[] iv = ivStr.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] cipherText = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // 3. Đóng gói Byte Buffer theo chuẩn Zego
            ByteBuffer buffer = ByteBuffer.allocate(8 + 2 + iv.length + 2 + cipherText.length);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(expireTime);
            buffer.putShort((short) iv.length);
            buffer.put(iv);
            buffer.putShort((short) cipherText.length);
            buffer.put(cipherText);

            // 4. Encode Base64 và thêm prefix "04"
            return "04" + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo Zego Token", e);
        }
    }
}