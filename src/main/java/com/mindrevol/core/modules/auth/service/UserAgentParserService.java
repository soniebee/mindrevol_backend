package com.mindrevol.core.modules.auth.service;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;
@Service
public class UserAgentParserService {
    private static final String UNKNOWN = "Unknown";
    private final Parser parser = new Parser();
    public UserAgentInfo parse(String rawUserAgent) {
        String userAgent = rawUserAgent == null ? "" : rawUserAgent.trim();
        if (userAgent.isBlank()) {
            return new UserAgentInfo(UNKNOWN, UNKNOWN, UNKNOWN);
        }
        Client client = parser.parse(userAgent);
        if (client == null) {
            return new UserAgentInfo(UNKNOWN, UNKNOWN, UNKNOWN);
        }
        String os = formatOs(client);
        String browser = formatBrowser(client);
        String deviceName = formatDevice(client);
        return new UserAgentInfo(os, browser, deviceName);
    }
    private String formatOs(Client client) {
        if (client.os == null || isBlank(client.os.family)) {
            return UNKNOWN;
        }
        String major = safePart(client.os.major);
        String minor = safePart(client.os.minor);
        String patch = safePart(client.os.patch);
        String version = joinVersion(major, minor, patch);
        return version.isBlank() ? client.os.family : client.os.family + " " + version;
    }
    private String formatBrowser(Client client) {
        if (client.userAgent == null || isBlank(client.userAgent.family)) {
            return UNKNOWN;
        }
        String major = safePart(client.userAgent.major);
        String minor = safePart(client.userAgent.minor);
        String version = joinVersion(major, minor);
        return version.isBlank() ? client.userAgent.family : client.userAgent.family + " " + version;
    }
    private String formatDevice(Client client) {
        if (client.device == null || isBlank(client.device.family) || "Other".equalsIgnoreCase(client.device.family)) {
            return UNKNOWN;
        }
        return client.device.family;
    }
    private String joinVersion(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (isBlank(part)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('.');
            }
            builder.append(part);
        }
        return builder.toString();
    }
    private String safePart(String value) {
        return value == null ? "" : value.trim();
    }
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
    public record UserAgentInfo(String os, String browser, String deviceName) {
    }
}
