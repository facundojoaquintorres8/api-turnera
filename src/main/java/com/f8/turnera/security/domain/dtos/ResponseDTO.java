package com.f8.turnera.security.domain.dtos;

import lombok.Data;

@Data
public class ResponseDTO {
    
    private String message;
    private Object data;

    public ResponseDTO(Object data, String message) {
        this.message = message;
        this.data = data;
    }

    public ResponseDTO(Object data) {
        this.data = data;
    }

}
