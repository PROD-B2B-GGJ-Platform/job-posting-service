package com.platform.talent.jobposting.service.integration;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KernelIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${integration.kernel.url:http://kernel-component:8080}")
    private String kernelUrl;

    @CircuitBreaker(name = "kernel", fallbackMethod = "storeExtendedAttributesFallback")
    @Retry(name = "kernel")
    public void storeExtendedAttributes(UUID objectId, String objectType, Map<String, Object> attributes) {
        try {
            String url = kernelUrl + "/api/v1/objects/" + objectId + "/attributes";
            Map<String, Object> request = Map.of(
                "objectType", objectType,
                "attributes", attributes
            );
            restTemplate.postForEntity(url, request, Void.class);
            log.info("Stored extended attributes for {} {}", objectType, objectId);
        } catch (Exception e) {
            log.error("Failed to store extended attributes", e);
            throw e;
        }
    }

    @CircuitBreaker(name = "kernel", fallbackMethod = "getExtendedAttributesFallback")
    @Retry(name = "kernel")
    public Map<String, Object> getExtendedAttributes(UUID objectId) {
        try {
            String url = kernelUrl + "/api/v1/objects/" + objectId + "/attributes";
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Failed to get extended attributes", e);
            return Map.of();
        }
    }

    // Fallback methods
    public void storeExtendedAttributesFallback(UUID objectId, String objectType, Map<String, Object> attributes, Exception e) {
        log.warn("Kernel service unavailable, using fallback for store attributes. Object: {}", objectId);
        // Attributes will remain in Job entity's customFields column
    }

    public Map<String, Object> getExtendedAttributesFallback(UUID objectId, Exception e) {
        log.warn("Kernel service unavailable, using fallback for get attributes. Object: {}", objectId);
        return Map.of();
    }
}

