package com.mindrevol.core.modules.auth.service.strategy;

public interface SocialLoginStrategy {
    String getProviderName(); // google, facebook, tiktok...

    // Input là Object để linh hoạt (String token hoặc Request Object)
    SocialProviderData verifyAndGetData(Object data);
}