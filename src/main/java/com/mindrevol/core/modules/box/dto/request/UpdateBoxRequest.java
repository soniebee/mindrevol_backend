package com.mindrevol.core.modules.box.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBoxRequest {
    private String name;
    private String description;
    private String themeSlug;
    private String avatar;
    private String textPosition;
}