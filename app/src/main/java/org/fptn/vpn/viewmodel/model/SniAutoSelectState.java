package org.fptn.vpn.viewmodel.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SniAutoSelectState {
    private final boolean isRunning;
    private final int progress; // A value from 0 to 100
    private final String statusMessage;
}
