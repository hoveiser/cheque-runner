package com.cheque.chequerunner.view;

import com.cheque.chequerunner.domain.Cheque;
import com.cheque.chequerunner.service.ChequeService;
import com.cheque.chequerunner.service.dto.ChequeIssueRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cheques")
public class ChequeController {

    private final ChequeService chequeService;

    public ChequeController(ChequeService chequeService) {
        this.chequeService = chequeService;
    }

    @PostMapping
    public ResponseEntity<Cheque> issueCheque(@Valid @RequestBody ChequeIssueRequest request) {
        Cheque issuedCheque = chequeService.issueCheque(request);
        return new ResponseEntity<>(issuedCheque, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/present")
    public ResponseEntity<Cheque> presentCheque(@PathVariable Long id) {
        Cheque presentedCheque = chequeService.presentCheque(id);
        return ResponseEntity.ok(presentedCheque);
    }
}