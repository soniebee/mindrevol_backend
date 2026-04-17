package com.mindrevol.core.modules.recap.service;

import com.mindrevol.core.modules.recap.dto.request.CreateGlobalRecapRequest;
import com.mindrevol.core.modules.recap.dto.request.CreateRecapRequest;
import com.mindrevol.core.modules.recap.entity.Recap;

import java.util.List;

public interface RecapService {
    
    List<Recap> getMyRecaps(String userId);
    
    Recap getRecapById(String recapId, String userId);
    
    void requestManualRecap(String journeyId, String userId, CreateRecapRequest request);
    
    void deleteRecap(String recapId, String userId);

    // Render Preview cho 1 Hành trình (tại Modal cũ)
    byte[] generatePreview(String journeyId, String userId, CreateRecapRequest request);

    // [THÊM MỚI] Render Preview cho Nhiều Hành trình (tại Modal Global mới)
    byte[] generateGlobalPreview(String userId, CreateGlobalRecapRequest request);
}