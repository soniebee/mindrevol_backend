package com.mindrevol.core.modules.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckinDeletedEvent {
    private String fileId; // ID của file trên Cloud cần xóa
}