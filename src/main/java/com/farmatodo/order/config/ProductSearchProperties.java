package com.farmatodo.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "products.search")
public record ProductSearchProperties(int minStock) {
}
