package com.gonnect.apiaide.apiselector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryTuple {

    private String plan;
    private String apiCall;
    private String response;
}
