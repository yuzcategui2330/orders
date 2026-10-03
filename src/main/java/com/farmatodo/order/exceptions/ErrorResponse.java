package com.farmatodo.order.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(@JsonProperty("message") String message) {
}
