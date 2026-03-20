package com.mindrevol.core.modules.checkin.event;

import com.mindrevol.core.modules.checkin.entity.Checkin;
import com.mindrevol.core.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentPostedEvent {
    private Checkin checkin;
    private User commenter;
    private String content;
}