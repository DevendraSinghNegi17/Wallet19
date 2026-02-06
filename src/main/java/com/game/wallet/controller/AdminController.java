package com.game.wallet.controller;

import com.game.wallet.dto.ApiResponse;
import com.game.wallet.service.LedgerReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LedgerReplayService ledgerReplayService;

    @PostMapping("/ledger/replay")
    public ResponseEntity<ApiResponse<Void>> replay() {
        ledgerReplayService.replayAll();
        return ResponseEntity.ok(ApiResponse.success("Ledger replay completed"));
    }
}
