package com.rafaelagamero.backend.dto;

import com.rafaelagamero.backend.model.Address;
import com.rafaelagamero.backend.model.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private Address address;
}
