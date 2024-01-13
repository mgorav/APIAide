package com.gonnect.apiaide.apiselector;

import com.gonnect.apiaide.oas.ReducedOpenAPISpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class APISelectorRequestInput {

    private String plan;

    private String background = "";

    private ReducedOpenAPISpec apiSpec;

}