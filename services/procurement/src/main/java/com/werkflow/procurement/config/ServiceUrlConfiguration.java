package com.werkflow.procurement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for external service URLs
 * These URLs are injected into Flowable process variables for RestServiceDelegate
 */
@Configuration
@ConfigurationProperties(prefix = "services")
@Getter
@Setter
public class ServiceUrlConfiguration {

    private ServiceUrl finance = new ServiceUrl();
    private ServiceUrl inventory = new ServiceUrl();
    private ServiceUrl hr = new ServiceUrl();
    private ServiceUrl engine = new ServiceUrl();

    @Getter
    @Setter
    public static class ServiceUrl {
        private String url;
    }

    public Map<String, String> getServiceUrlMap() {
        Map<String, String> urls = new HashMap<>();
        urls.put("financeServiceUrl", finance.getUrl());
        urls.put("inventoryServiceUrl", inventory.getUrl());
        urls.put("hrServiceUrl", hr.getUrl());
        urls.put("engineServiceUrl", engine.getUrl());
        return urls;
    }
}
