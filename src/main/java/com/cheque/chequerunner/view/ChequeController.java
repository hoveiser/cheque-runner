package com.cheque.chequerunner.view;

import com.cheque.chequerunner.domain.Cheque;
import com.cheque.chequerunner.service.ChequeService;
import com.cheque.chequerunner.service.dto.ChequeIssueRequest;
import com.cheque.chequerunner.service.dto.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cheques")
@Tag(name = "Cheque Operations", description = "Endpoints for issuing and presenting cheques.")
public class ChequeController {

    private final ChequeService chequeService;

    public ChequeController(ChequeService chequeService) {
        this.chequeService = chequeService;
    }

    @Operation(summary = "Issues a new cheque", description = "Requires authentication and sufficient account balance.")
    @PostMapping
    public ResponseEntity<Cheque> issueCheque(@Valid @RequestBody ChequeIssueRequest request) {
        Cheque issuedCheque = chequeService.issueCheque(request);
        return new ResponseEntity<>(issuedCheque, HttpStatus.CREATED);
    }

    @Operation(summary = "Present an existing cheque", description = "Requires authentication and sufficient account balance.")
    @PostMapping("/{id}/present")
    public ResponseEntity<ResponseMessage> presentCheque(@PathVariable Long id) {
        ResponseMessage responseMessage = chequeService.presentCheque(id);
        return new ResponseEntity<>(responseMessage, responseMessage.getStatusCode());
    }
}