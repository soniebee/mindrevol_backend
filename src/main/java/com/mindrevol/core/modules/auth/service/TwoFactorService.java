package com.mindrevol.core.modules.auth.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.Base64;
import java.util.Locale;

@Service
public class TwoFactorService {

    private static final char[] BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String buildOtpAuthUri(String issuer, String accountName, String secret) {
        String safeIssuer = urlEncode(issuer);
        String safeAccount = urlEncode(accountName);
        return "otpauth://totp/" + safeIssuer + ":" + safeAccount
                + "?secret=" + secret
                + "&issuer=" + safeIssuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    /**
     * Generates a QR code image from the OTP Auth URI as Base64-encoded PNG.
     * This can be displayed in the frontend to allow users to scan it with authenticator apps.
     *
     * @param otpAuthUri the URI created by buildOtpAuthUri
     * @return Base64-encoded PNG image of the QR code
     * @throws IllegalStateException if QR code generation fails
     */
    public String generateQrCodeImage(String otpAuthUri) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUri, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return Base64.getEncoder().encodeToString(pngData);
        } catch (WriterException | java.io.IOException ex) {
            throw new IllegalStateException("Failed to generate QR code image", ex);
        }
    }

    public boolean verifyCode(String secret, String code, int window) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long timeWindow = System.currentTimeMillis() / 1000L / 30L;
        for (int i = -window; i <= window; i++) {
            String generated = generateCode(secret, timeWindow + i);
            if (code.equals(generated)) {
                return true;
            }
        }
        return false;
    }

    public List<String> generateBackupCodes(int count) {
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            codes.add(randomBackupCode());
        }
        return codes;
    }

    public String hashCode(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to hash backup code", ex);
        }
    }

    private String randomBackupCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private String generateCode(String secret, long counter) {
        byte[] key = base32Decode(secret);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (counter & 0xFF);
            counter >>= 8;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hmac = mac.doFinal(data);
            int offset = hmac[hmac.length - 1] & 0x0F;
            int binary = ((hmac[offset] & 0x7F) << 24)
                    | ((hmac[offset + 1] & 0xFF) << 16)
                    | ((hmac[offset + 2] & 0xFF) << 8)
                    | (hmac[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to generate TOTP", ex);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer <<= 8;
            buffer |= (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                result.append(BASE32_CHARS[index]);
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_CHARS[index]);
        }
        return result.toString();
    }

    private byte[] base32Decode(String input) {
        String normalized = input.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        byte[] bytes = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;

        for (char c : normalized.toCharArray()) {
            int value = base32Value(c);
            if (value < 0) {
                continue;
            }
            buffer <<= 5;
            buffer |= value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        if (count == bytes.length) {
            return bytes;
        }
        byte[] result = new byte[count];
        System.arraycopy(bytes, 0, result, 0, count);
        return result;
    }

    private int base32Value(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= '2' && c <= '7') {
            return c - '2' + 26;
        }
        return -1;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
