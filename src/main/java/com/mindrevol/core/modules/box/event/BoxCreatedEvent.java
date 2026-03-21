package com.mindrevol.core.modules.box.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoxCreatedEvent {
    private String boxId;
    private String boxName;
    private String ownerId;

    // Danh sách ID thành viên (bao gồm chủ phòng và những người được mời thành công lúc tạo)
    // Gửi cái list này đi để bạn làm module Chat tự động add hết đống này vào Group Chat 1 lượt luôn
    private List<String> memberIds;
}