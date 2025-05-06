package me.roinujnosde.titansbattle.games;

import me.roinujnosde.titansbattle.TitansBattle;
import me.roinujnosde.titansbattle.types.GameConfiguration;
import me.roinujnosde.titansbattle.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Boxing extends EliminationTournamentGame {

    private final Map<UUID, Integer> hitsCount = new HashMap<>();

    public Boxing(TitansBattle plugin, GameConfiguration config) {
        super(plugin, config);
    }

    public boolean onHit(@NotNull Player attacker, Player victim) {
        UUID attackerUUID = attacker.getUniqueId();
        hitsCount.put(attackerUUID, hitsCount.getOrDefault(attackerUUID, 0) + 1);
        if (hitsCount.get(attackerUUID) < getConfig().getHitAmount()) {
            MessageUtils.sendActionBar(attacker, getLang("boxing_hit_count", hitsCount.get(attackerUUID), getConfig().getHitAmount()));
            return true;
        } else {
            hitsCount.remove(attackerUUID);
            hitsCount.remove(victim.getUniqueId());
            plugin.debug(String.format("onHit() - kill player %s", victim.getName()));
            return false;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Boxing boxing)) return false;
        if (!super.equals(o)) return false;

        return hitsCount.equals(boxing.hitsCount);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + hitsCount.hashCode();
        return result;
    }
}
