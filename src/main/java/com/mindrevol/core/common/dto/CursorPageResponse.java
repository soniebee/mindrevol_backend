// File: src/main/java/com/mindrevol/backend/common/dto/CursorPageResponse.java
package com.mindrevol.core.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponse<T> {
    private List<T> data;
    private String nextCursor; // ID hoặc Timestamp của phần tử cuối cùng
    private boolean hasNext;   // Còn dữ liệu để load tiếp không?
}