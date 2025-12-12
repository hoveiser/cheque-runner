package com.cheque.chequerunner.service;

import com.cheque.chequerunner.domain.Account;
import com.cheque.chequerunner.domain.BounceRecord;
import com.cheque.chequerunner.domain.Cheque;
import com.cheque.chequerunner.repository.AccountRepository;
import com.cheque.chequerunner.repository.BounceRecordRepository;
import com.cheque.chequerunner.repository.ChequeRepository;
import com.cheque.chequerunner.service.dto.ChequeIssueRequest;
import com.cheque.chequerunner.service.dto.ResponseMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

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

        Optional<Cheque> existedCheque = chequeRepository.findByNumberAndDrawerId(request.getNumber(), drawer.getId());

        if(existedCheque.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cheque already exists");
        }

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
    public ResponseMessage presentCheque(Long chequeId) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cheque not found"));

        if(Cheque.ChequeStatus.PAID.equals(cheque.getChequeStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cheque already paid.");
        }

        if(Cheque.ChequeStatus.BOUNCED.equals(cheque.getChequeStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cheque already bounced.");
        }

        Account drawer = cheque.getDrawer();

        boolean sayadChequeValid = sayadClient.present(cheque.getNumber());
        if (!sayadChequeValid) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Cheque not valid by Sayad service");
        }

        if (LocalDate.now().isAfter(cheque.getIssueDate().plusMonths(6))) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Cheque validity window expired (over 6 months).");
        }

        if (drawer.getBalance().compareTo(cheque.getAmount()) < 0) {

            String message="Insufficient Funds";
            commitBounce(cheque, drawer.getId(), message);

            return new ResponseMessage("Cheque bounced.", HttpStatus.CONFLICT,message);
        }

        drawer.setBalance(drawer.getBalance().subtract(cheque.getAmount()));
        accountRepository.save(drawer);

        cheque.setChequeStatus(Cheque.ChequeStatus.PAID);
        chequeRepository.save(cheque);
        return new ResponseMessage("cheque paid.", HttpStatus.OK,"Sufficient Funds");
    }

    private long countBouncesInLast12Months(Long drawerId) {
        return bounceRepository.countBouncesByDrawerAndDateAfter(drawerId, LocalDate.now().minusYears(1));
    }

    private void commitBounce(Cheque cheque, Long drawerId, String reason) {
        BounceRecord bounceRecord = new BounceRecord();
        bounceRecord.setChequeId(cheque.getId());
        bounceRecord.setDate(LocalDate.now());
        bounceRecord.setReason(reason);
        bounceRepository.save(bounceRecord);

        cheque.setChequeStatus(Cheque.ChequeStatus.BOUNCED);
        chequeRepository.save(cheque);


        long bounceCount = countBouncesInLast12Months(drawerId);
        if (bounceCount >= 2) {
            Account drawer = cheque.getDrawer();
            if (drawer != null) {
                drawer.setAccountStatus(Account.AccountStatus.BLOCKED);
                accountRepository.save(drawer);
            }
        }
    }
}
