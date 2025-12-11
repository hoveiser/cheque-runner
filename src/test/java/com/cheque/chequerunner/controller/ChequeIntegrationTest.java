package com.cheque.chequerunner.controller;

import com.cheque.chequerunner.ChequeRunnerApplication;
import com.cheque.chequerunner.domain.Account;
import com.cheque.chequerunner.repository.AccountRepository;
import com.cheque.chequerunner.repository.ChequeRepository;
import com.cheque.chequerunner.service.dto.ChequeIssueRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ChequeRunnerApplication.class)
class ChequeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ChequeRepository chequeRepository;

    private HttpHeaders headers;
    private String baseUrl;
    private Account drawer;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/cheques";

        headers = new HttpHeaders();
        headers.setBasicAuth("teller1", "teller");
        headers.setContentType(MediaType.APPLICATION_JSON);

        drawer = new Account();
        drawer.setBalance(new BigDecimal("5000.00"));
        drawer.setAccountStatus(Account.AccountStatus.ACTIVE);
        drawer = accountRepository.save(drawer);

    }

    @AfterEach
    void tearDown() {
        chequeRepository.deleteAll();
        accountRepository.deleteAll();
    }


    @Test
    void issueAndPresentCheque_ShouldSucceedAndTransferFunds() {
        ChequeIssueRequest issueRequest = new ChequeIssueRequest(drawer.getId(), "YT-2025-0001", new BigDecimal("1000.00"));
        HttpEntity<ChequeIssueRequest> issueEntity = new HttpEntity<>(issueRequest, headers);

        ResponseEntity<Account> issueResponse = restTemplate.exchange(
                baseUrl, HttpMethod.POST, issueEntity, Account.class);

        assertEquals(HttpStatus.CREATED, issueResponse.getStatusCode());

        Long issuedChequeId = chequeRepository.findAll().get(0).getId();

        HttpEntity<String> presentEntity = new HttpEntity<>(null, headers);
        ResponseEntity<Account> presentResponse = restTemplate.exchange(
                baseUrl + "/" + issuedChequeId + "/present", HttpMethod.POST, presentEntity, Account.class);

        assertEquals(HttpStatus.OK, presentResponse.getStatusCode());

        Account finalDrawer = accountRepository.findById(drawer.getId()).orElseThrow();

        assertEquals(new BigDecimal("4000.00"), finalDrawer.getBalance());
    }


    @Test
    void presentCheque_ShouldReturnConflict_WhenInsufficientBalance() {
        ChequeIssueRequest issueRequest = new ChequeIssueRequest(drawer.getId(), "YT-2025-0002", new BigDecimal("6000.00"));
        HttpEntity<ChequeIssueRequest> issueEntity = new HttpEntity<>(issueRequest, headers);

        ResponseEntity<Account> presentResponse =restTemplate.exchange(baseUrl, HttpMethod.POST, issueEntity, Account.class);

        assertEquals(HttpStatus.BAD_REQUEST, presentResponse.getStatusCode());


        Account finalDrawer = accountRepository.findById(drawer.getId()).orElseThrow();
        assertEquals(new BigDecimal("5000.00"), finalDrawer.getBalance()); // باید همان 5000 بماند
    }


    @Test
    void issueCheque_ShouldFail_WhenInvalidChequeNumberFormat() {
        ChequeIssueRequest issueRequest = new ChequeIssueRequest(drawer.getId(), "XX", new BigDecimal("100.00"));
        HttpEntity<ChequeIssueRequest> issueEntity = new HttpEntity<>(issueRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl, HttpMethod.POST, issueEntity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        assertTrue(Objects.requireNonNull(response.getBody()).contains("Cheque number format must be: Two-Letters-Four-Digits-Four-Digits"));
    }
}