package com.mindrevol.core.modules.chat.entity;

public enum MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    VOICE,  // <--- THÊM CHỮ NÀY VÀO ĐỂ BACKEND NHẬN DIỆN ĐƯỢC
    FILE,
    SYSTEM
}