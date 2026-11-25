package com.leaseflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Embedded in {@link PropertyRequest} and standalone unit creation. */
@Getter @Setter
public class UnitRequest {

    @NotBlank(message = "Unit number is required")
    @Size(max = 50)
    private String unitNumber;

    @Size(max = 10)
    private String floor;
}

