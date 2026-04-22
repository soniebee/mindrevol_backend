package com.mindrevol.core.modules.recap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecapTask implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String recapId;
    private String journeyId;
    private String userId;
    
    // THÊM 3 TRƯỜNG NÀY ĐỂ WORKER BIẾT ĐƯỜNG RENDER:
    private Integer speedDelayMs; 
    private String filterType;
    private List<String> selectedCheckinIds;
}