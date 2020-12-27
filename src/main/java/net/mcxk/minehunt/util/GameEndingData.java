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
    private final String damageReceive;
    private final String runnerKiller;
    private final String stoneAgePassed;



}
