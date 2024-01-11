package com.gonnect.apiaide.apiaide.apiselector;

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

    private String background;

}