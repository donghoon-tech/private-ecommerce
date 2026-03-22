package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"
)
public class MenuDTO {
    private UUID id;
    private String menuCode;
    private String name;
    private UUID parentId;
    private int sortOrder;
    
    @JsonProperty("isVisible")
    private boolean isVisible;
    
    // For building recursive trees in the UI
    private List<MenuDTO> children;
}
