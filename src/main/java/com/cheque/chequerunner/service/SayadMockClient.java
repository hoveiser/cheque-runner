package com.cheque.chequerunner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SayadMockClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sayad.register.url}")
    private String registerUrl;

    @Value("${sayad.present.url}")
    private String presentUrl;

    public boolean register(String chequeNumber) {
        System.out.println("SAYAD Mock: Registering cheque number " + chequeNumber + " at " + registerUrl);
        return true;
    }

    public boolean present(String chequeNumber) {
        System.out.println("SAYAD Mock: Presenting cheque number " + chequeNumber + " at " + presentUrl);
        return true;
    }
}