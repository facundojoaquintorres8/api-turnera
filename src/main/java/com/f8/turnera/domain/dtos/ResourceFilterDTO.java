package com.f8.turnera.domain.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceFilterDTO extends DefaultFilterDTO {

    private Long resourceTypeId;
    private String description;
    private String code;
    
}
