package com.farmatodo.order.domains.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PayOrderRequest(@JsonProperty("token") String token) {
}
