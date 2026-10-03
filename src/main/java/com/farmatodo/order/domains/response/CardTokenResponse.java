package com.farmatodo.order.domains.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardTokenResponse(@JsonProperty("token") String token) {
}
