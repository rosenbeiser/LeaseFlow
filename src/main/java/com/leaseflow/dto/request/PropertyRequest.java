package com.leaseflow.dto.request;

import com.leaseflow.enums.PropertyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Request body for {@code POST /api/v1/properties}. Includes initial units. */
@Getter @Setter
public class PropertyRequest {

    @NotBlank(message = "Property name is required")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 500)
    private String address;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @Valid
    @NotEmpty(message = "At least one unit is required")
    private List<UnitRequest> units = new ArrayList<>();
}

