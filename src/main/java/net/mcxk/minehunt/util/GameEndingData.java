package net.mcxk.minehunt.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class GameEndingData {
    private final String dragonKiller;
    private final String damageOutput;
    private final String strongHoldFinder;
    private final String netherFortressFinder;
    private final String damageReceive;
    private final String runnerKiller;

}
