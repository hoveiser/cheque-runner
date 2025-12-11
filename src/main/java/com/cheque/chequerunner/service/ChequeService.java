package com.cheque.chequerunner.service;

import com.cheque.chequerunner.domain.Account;
import com.cheque.chequerunner.domain.BounceRecord;
import com.cheque.chequerunner.domain.Cheque;
import com.cheque.chequerunner.repository.AccountRepository;
import com.cheque.chequerunner.repository.BounceRecordRepository;
import com.cheque.chequerunner.repository.ChequeRepository;
import com.cheque.chequerunner.service.dto.ChequeIssueRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ChequeService {

    private final AccountRepository accountRepository;
    private final ChequeRepository chequeRepository;
    private final BounceRecordRepository bounceRepository;
    private final SayadMockClient sayadClient;


    @Transactional
    public Cheque issueCheque(ChequeIssueRequest request) {
        Account drawer = accountRepository.findById(request.getDrawerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Drawer account not found"));


        if (drawer.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance to issue cheque.");
        }

        boolean registerStatus = sayadClient.register(request.getNumber());

        if (!registerStatus) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Drawer Sayad Cheque not registered");
        }

        Cheque cheque = new Cheque();
        cheque.setNumber(request.getNumber());
        cheque.setAmount(request.getAmount());
        cheque.setDrawer(drawer);
        cheque.setIssueDate(LocalDate.now());
        cheque.setChequeStatus(Cheque.ChequeStatus.ISSUED);
        return chequeRepository.save(cheque);
    }

    @Transactional
    public Cheque presentCheque(Long chequeId) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cheque not found"));

        Account drawer = cheque.getDrawer();

        boolean sayadChequeValid = sayadClient.present(cheque.getNumber());
        if (!sayadChequeValid) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Cheque not valid by Sayad service");
        }

        if (LocalDate.now().isAfter(cheque.getIssueDate().plusMonths(6))) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Cheque validity window expired (over 6 months).");
        }

        if (drawer.getBalance().compareTo(cheque.getAmount()) < 0) {
            createBounceRecord(chequeId, "Insufficient Funds");

            cheque.setChequeStatus(Cheque.ChequeStatus.BOUNCED);
            chequeRepository.save(cheque);

            long bounceCount = countBouncesInLast12Months(drawer.getId());

            if (bounceCount >= 2) {
                drawer.setAccountStatus(Account.AccountStatus.BLOCKED);
                accountRepository.save(drawer);
            }

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cheque bounced.");
        }

        drawer.setBalance(drawer.getBalance().subtract(cheque.getAmount()));
        accountRepository.save(drawer);

        cheque.setChequeStatus(Cheque.ChequeStatus.PAID);
        return chequeRepository.save(cheque);
    }

    private void createBounceRecord(Long chequeId, String reason) {
        BounceRecord bounceRecord = new BounceRecord();
        bounceRecord.setChequeId(chequeId);
        bounceRecord.setDate(LocalDate.now());
        bounceRecord.setReason(reason);
        bounceRepository.save(bounceRecord);
    }

    private long countBouncesInLast12Months(Long drawerId) {
        return bounceRepository.countBouncesByDrawerAndDateAfter(drawerId, LocalDate.now().minusYears(1));
    }
}
