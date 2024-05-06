package com.f8.turnera.security.models;

import com.f8.turnera.models.DefaultFilterDTO;

import lombok.Data;

@Data
public class UserFilterDTO extends DefaultFilterDTO {

    private String firstName;
    private String lastName;
    private String username;

}