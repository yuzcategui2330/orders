package com.farmatodo.order.domains.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateOrderRequest(@JsonProperty("client_id") Long clientId) {
}
