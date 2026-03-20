package com.mindrevol.core.modules.chat.dto.request;

import lombok.Data;

@Data
public class WebRtcMessage {
    private String type;     // call-request, call-accept, call-reject, offer, answer, ice-candidate, end-call
    private String senderId;
    private String targetId;
    private String sdp;
    private Object candidate;
}