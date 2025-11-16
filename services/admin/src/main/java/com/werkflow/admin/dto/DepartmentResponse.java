package com.werkflow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Long organizationId;
    private String organizationName;
    private Long parentDepartmentId;
    private String parentDepartmentName;
    private String managerUserId;
    private String location;
    private String phone;
    private String email;
    private Boolean active;
    private List<DepartmentResponse> childDepartments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
