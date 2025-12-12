package com.cheque.chequerunner.controller;

import com.cheque.chequerunner.ChequeRunnerApplication;
import com.cheque.chequerunner.domain.Account;
import com.cheque.chequerunner.domain.Cheque;
import com.cheque.chequerunner.repository.AccountRepository;
import com.cheque.chequerunner.repository.BounceRecordRepository;
import com.cheque.chequerunner.repository.ChequeRepository;
import com.cheque.chequerunner.service.dto.ChequeIssueRequest;
import com.cheque.chequerunner.service.dto.ResponseMessage;
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
import java.util.Optional;

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

    @Autowired
    private BounceRecordRepository bounceRecordRepository;

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
        ResponseEntity<Cheque> presentResponse = restTemplate.exchange(
                baseUrl + "/" + issuedChequeId + "/present", HttpMethod.POST, presentEntity, Cheque.class);

        assertEquals(HttpStatus.OK, presentResponse.getStatusCode());
        assertEquals(Cheque.ChequeStatus.PAID, chequeRepository.findById(issuedChequeId).orElseThrow().getChequeStatus());

        Account finalDrawer = accountRepository.findById(drawer.getId()).orElseThrow();

        assertEquals(new BigDecimal("4000.00"), finalDrawer.getBalance());
    }


    @Test
    void presentCheque_ShouldReturnConflict_WhenInsufficientBalance() {
        ChequeIssueRequest issueRequest = new ChequeIssueRequest(drawer.getId(), "YT-2025-0002", new BigDecimal("6000.00"));
        HttpEntity<ChequeIssueRequest> issueEntity = new HttpEntity<>(issueRequest, headers);

        ResponseEntity<Account> presentResponse = restTemplate.exchange(baseUrl, HttpMethod.POST, issueEntity, Account.class);

        assertEquals(HttpStatus.BAD_REQUEST, presentResponse.getStatusCode());


        Account finalDrawer = accountRepository.findById(drawer.getId()).orElseThrow();
        assertEquals(new BigDecimal("5000.00"), finalDrawer.getBalance());
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

    @Test
    void presentCheque_ShouldFailAndBounceAndBlocked_WhenInsufficientBalance() throws Exception {
        ChequeIssueRequest issueRequest1 = new ChequeIssueRequest(drawer.getId(), "YT-2025-0001", new BigDecimal("1000.00"));
        ChequeIssueRequest issueRequest2 = new ChequeIssueRequest(drawer.getId(), "YT-2025-0002", new BigDecimal("4500.00"));
        ChequeIssueRequest issueRequest3 = new ChequeIssueRequest(drawer.getId(), "YT-2025-0003", new BigDecimal("4600.00"));
        ChequeIssueRequest issueRequest4 = new ChequeIssueRequest(drawer.getId(), "YT-2025-0004", new BigDecimal("4700.00"));
        HttpEntity<ChequeIssueRequest> issueEntity = new HttpEntity<>(issueRequest1, headers);
        HttpEntity<ChequeIssueRequest> issueEntity2 = new HttpEntity<>(issueRequest2, headers);
        HttpEntity<ChequeIssueRequest> issueEntity3 = new HttpEntity<>(issueRequest3, headers);
        HttpEntity<ChequeIssueRequest> issueEntity4 = new HttpEntity<>(issueRequest4, headers);

        ResponseEntity<Account> issueResponse = restTemplate.exchange(
                baseUrl, HttpMethod.POST, issueEntity, Account.class);

        ResponseEntity<Account> issueResponse2 = restTemplate.exchange(
                baseUrl, HttpMethod.POST, issueEntity2, Account.class);

        ResponseEntity<Account> issueResponse3 = restTemplate.exchange(
                baseUrl, HttpMethod.POST, issueEntity3, Account.class);

        ResponseEntity<Account> issueResponse4 = restTemplate.exchange(
                baseUrl, HttpMethod.POST, issueEntity4, Account.class);

        assertEquals(HttpStatus.CREATED, issueResponse.getStatusCode());
        assertEquals(HttpStatus.CREATED, issueResponse2.getStatusCode());
        assertEquals(HttpStatus.CREATED, issueResponse3.getStatusCode());
        assertEquals(HttpStatus.CREATED, issueResponse4.getStatusCode());
        Long issuedChequeId1 = chequeRepository.findAll().get(0).getId();
        Long issuedChequeId2 = chequeRepository.findAll().get(1).getId();
        Long issuedChequeId3 = chequeRepository.findAll().get(2).getId();
        Long issuedChequeId4 = chequeRepository.findAll().get(3).getId();


        HttpEntity<String> presentEntity = new HttpEntity<>(null, headers);
        ResponseEntity<ResponseMessage> presentResponse1 = restTemplate.exchange(
                baseUrl + "/" + issuedChequeId1 + "/present", HttpMethod.POST, presentEntity, ResponseMessage.class);
        ResponseEntity<ResponseMessage> presentResponse2 = restTemplate.exchange(
                baseUrl + "/" + issuedChequeId2 + "/present", HttpMethod.POST, presentEntity, ResponseMessage.class);
        ResponseEntity<ResponseMessage> presentResponse3 = restTemplate.exchange(
                baseUrl + "/" + issuedChequeId3 + "/present", HttpMethod.POST, presentEntity, ResponseMessage.class);
        ResponseEntity<ResponseMessage> presentResponse4 = restTemplate.exchange(
                baseUrl + "/" + issuedChequeId4 + "/present", HttpMethod.POST, presentEntity, ResponseMessage.class);


        assertEquals(HttpStatus.OK, presentResponse1.getStatusCode());
        assertEquals(Cheque.ChequeStatus.PAID, chequeRepository.findById(issuedChequeId1).orElseThrow().getChequeStatus());
        assertEquals(HttpStatus.CONFLICT, presentResponse2.getStatusCode());
        assertEquals(Cheque.ChequeStatus.BOUNCED, chequeRepository.findById(issuedChequeId2).orElseThrow().getChequeStatus());
        assertEquals(HttpStatus.CONFLICT, presentResponse3.getStatusCode());
        assertEquals(Cheque.ChequeStatus.BOUNCED, chequeRepository.findById(issuedChequeId2).orElseThrow().getChequeStatus());
        assertEquals(HttpStatus.CONFLICT, presentResponse4.getStatusCode());
        assertEquals(Cheque.ChequeStatus.BOUNCED, chequeRepository.findById(issuedChequeId2).orElseThrow().getChequeStatus());


        Optional<Cheque> bouncedCheque = chequeRepository.findById(issuedChequeId2);
        assertTrue(bouncedCheque.isPresent(), "Cheque should exist after presentation attempt.");
        assertEquals(Cheque.ChequeStatus.BOUNCED, bouncedCheque.get().getChequeStatus(), "Cheque status must be BOUNCED.");

        Account account = accountRepository.findById(drawer.getId()).orElseThrow();
        assertEquals(new BigDecimal("4000.00"), account.getBalance(),
                "Drawer balance should remain unchanged (4000.00).");
        assertEquals(Account.AccountStatus.BLOCKED, account.getAccountStatus(), "Account status must be BLOCKED.");

        assertEquals(3, bounceRecordRepository.count(), "bounce record should have exactly one record");
    }
}