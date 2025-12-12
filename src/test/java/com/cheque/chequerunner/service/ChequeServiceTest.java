package com.cheque.chequerunner.service;

import com.cheque.chequerunner.domain.Account;
import com.cheque.chequerunner.domain.Cheque;
import com.cheque.chequerunner.repository.AccountRepository;
import com.cheque.chequerunner.repository.BounceRecordRepository;
import com.cheque.chequerunner.repository.ChequeRepository;
import com.cheque.chequerunner.service.dto.ChequeIssueRequest;
import com.cheque.chequerunner.service.dto.ResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChequeServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ChequeRepository chequeRepository;

    @Mock
    private BounceRecordRepository bounceRepository;

    @Mock
    private SayadMockClient sayadClient;

    @InjectMocks
    private ChequeService chequeService;

    private Account drawer;

    private Cheque cheque;

    @BeforeEach
    void setUp() {
        drawer = new Account();
        drawer.setId(1L);
        drawer.setBalance(new BigDecimal("2000.00"));
        drawer.setAccountStatus(Account.AccountStatus.ACTIVE);

        cheque = new Cheque();
        cheque.setId(2L);
        cheque.setDrawer(drawer);
        cheque.setAmount(new BigDecimal("100.00"));
        cheque.setNumber("YT-2025-0011");
        cheque.setChequeStatus(Cheque.ChequeStatus.ISSUED);
        cheque.setIssueDate(LocalDate.now());
    }

    @Test
    void issueCheque_ShouldSucceed() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(drawer));
        when(sayadClient.register(any())).thenReturn(true);

        ChequeIssueRequest request = new ChequeIssueRequest(1L, "YT-2025-0001", new BigDecimal("500.00"));

        when(chequeRepository.save(any(Cheque.class))).thenAnswer(i -> i.getArgument(0));

        Cheque result = chequeService.issueCheque(request);

        assertNotNull(result);
        assertEquals(Cheque.ChequeStatus.ISSUED, result.getChequeStatus());
        verify(sayadClient, times(1)).register("YT-2025-0001");
    }

    @Test
    void issueCheque_ShouldThrowException_WhenInsufficientBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(drawer));

        ChequeIssueRequest request = new ChequeIssueRequest(1L, "YT-2025-0002", new BigDecimal("3000.00"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> chequeService.issueCheque(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Insufficient balance to issue cheque.",exception.getReason());
        verify(chequeRepository, never()).save(any());
    }

    @Test
    void presentCheque_ShouldSucceed() {
        when(sayadClient.present(any())).thenReturn(true);

        cheque.setIssueDate(LocalDate.now());

        when(chequeRepository.findById(10L)).thenReturn(Optional.of(cheque));
        when(chequeRepository.save(any(Cheque.class))).thenAnswer(i -> i.getArgument(0));

        ResponseMessage responseMessage = chequeService.presentCheque(10L);


        assertEquals("cheque paid.",responseMessage.getMessage());
        assertEquals("Sufficient Funds",responseMessage.getReason());
        assertEquals(HttpStatus.OK, responseMessage.getStatusCode());
        assertEquals(new BigDecimal("1900.00"), drawer.getBalance());
    }

    @Test
    void presentCheque_ShouldThrowException_WhenSayadPresentFailed() {
        when(sayadClient.present(any())).thenReturn(false);

        cheque.setIssueDate(LocalDate.now());

        when(chequeRepository.findById(2L)).thenReturn(Optional.of(cheque));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> chequeService.presentCheque(2L));

        assertEquals(Cheque.ChequeStatus.ISSUED, cheque.getChequeStatus());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getStatusCode());
        assertEquals("Cheque not valid by Sayad service",exception.getReason());
        verify(chequeRepository, never()).save(any());
    }


    @Test
    void presentCheque_ShouldBounce_WhenExpired() {
        when(sayadClient.present(any())).thenReturn(true);
        when(chequeRepository.findById(2L)).thenReturn(Optional.of(cheque));

        cheque.setChequeStatus(Cheque.ChequeStatus.ISSUED);
        cheque.setIssueDate(LocalDate.now().minusMonths(6).minusDays(1));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> chequeService.presentCheque(2L));

        assertEquals(Cheque.ChequeStatus.ISSUED, cheque.getChequeStatus());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getStatusCode());
        assertEquals("Cheque validity window expired (over 6 months).",exception.getReason());
        verify(chequeRepository, never()).save(any());
    }

    @Test
    void presentCheque_ShouldBounceAndBlockAccount_WhenThirdBounceOccurs() {
        when(sayadClient.present(any())).thenReturn(true);
        cheque.setAmount(new BigDecimal("3000.00"));
        when(chequeRepository.findById(12L)).thenReturn(Optional.of(cheque));

        when(bounceRepository.countBouncesByDrawerAndDateAfter(eq(1L), any(LocalDate.class))).thenReturn(2L);

        ResponseMessage responseMessage = chequeService.presentCheque(12L);

        verify(accountRepository, times(1)).save(drawer);
        assertEquals(HttpStatus.CONFLICT, responseMessage.getStatusCode());
        assertEquals("Cheque bounced.",responseMessage.getMessage());
        assertEquals(Account.AccountStatus.BLOCKED, drawer.getAccountStatus());
        assertEquals(Cheque.ChequeStatus.BOUNCED, cheque.getChequeStatus());
    }

    @Test
    void issueCheque_ShouldReject_WhenIssuedBefore() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(drawer));
        when(sayadClient.register(any())).thenReturn(true);

        ChequeIssueRequest request = new ChequeIssueRequest(1L, "YT-2025-0001", new BigDecimal("500.00"));

        when(chequeRepository.save(any(Cheque.class))).thenAnswer(i -> i.getArgument(0));

        Cheque result = chequeService.issueCheque(request);

        assertNotNull(result);
        assertEquals(Cheque.ChequeStatus.ISSUED, result.getChequeStatus());
        verify(sayadClient, times(1)).register("YT-2025-0001");

        when(chequeRepository.findByNumberAndDrawerId(any(),any())).thenReturn(Optional.of(cheque));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> chequeService.issueCheque(request));
        assertEquals("Cheque already exists", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void presentCheque_ShouldReject_WhenPaidBefore() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(drawer));
        when(sayadClient.register(any())).thenReturn(true);

        ChequeIssueRequest request = new ChequeIssueRequest(1L, "YT-2025-0001", new BigDecimal("500.00"));

        when(chequeRepository.save(any(Cheque.class))).thenAnswer(i -> i.getArgument(0));
        Cheque result = chequeService.issueCheque(request);
        when(chequeRepository.findById(result.getId())).thenReturn(Optional.of(cheque));


        assertNotNull(result);
        assertEquals(Cheque.ChequeStatus.ISSUED, result.getChequeStatus());
        verify(sayadClient, times(1)).register("YT-2025-0001");
        cheque.setChequeStatus(Cheque.ChequeStatus.PAID);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> chequeService.presentCheque(result.getId()));
        assertEquals("Cheque already paid.", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void presentCheque_ShouldReject_WhenBounceBefore() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(drawer));
        when(sayadClient.register(any())).thenReturn(true);

        ChequeIssueRequest request = new ChequeIssueRequest(1L, "YT-2025-0001", new BigDecimal("500.00"));

        when(chequeRepository.save(any(Cheque.class))).thenAnswer(i -> i.getArgument(0));
        Cheque result = chequeService.issueCheque(request);
        when(chequeRepository.findById(result.getId())).thenReturn(Optional.of(cheque));


        assertNotNull(result);
        assertEquals(Cheque.ChequeStatus.ISSUED, result.getChequeStatus());
        verify(sayadClient, times(1)).register("YT-2025-0001");
        cheque.setChequeStatus(Cheque.ChequeStatus.BOUNCED);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> chequeService.presentCheque(result.getId()));
        assertEquals("Cheque already bounced.", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

}