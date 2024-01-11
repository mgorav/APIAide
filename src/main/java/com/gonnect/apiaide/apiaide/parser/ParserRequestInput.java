package com.gonnect.apiaide.apiaide.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ParserRequestInput {

    private String query;
    private String json;
    private String apiParam;
    private String apiPath;
    private String apiDescription;
    private String responseDescription;  // Added field for response_description

}
