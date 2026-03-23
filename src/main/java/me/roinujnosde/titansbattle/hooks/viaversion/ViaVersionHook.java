package me.roinujnosde.titansbattle.hooks.viaversion;

import com.viaversion.viaversion.api.Via;
import me.roinujnosde.titansbattle.BaseGameConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ViaVersionHook {

    public ViaVersionHook() {
    }

    /**
     * Verifica se a versão de protocolo do jogador está contida na lista de protocolos bloqueados.
     *
     * @param player o jogador que será verificado
     * @param config a configuração do jogo
     * @return true se a versão do protocolo do jogador estiver na lista blockedProtocols; caso contrário, false.
     */
    public boolean isPlayerVersionBlocked(final @NotNull Player player, final @NotNull BaseGameConfiguration config) {
        final int playerProtocolVersion = Via.getAPI().getPlayerVersion(player.getUniqueId());
        final List<Integer> blockedProtocols = config.getBlockedProtocols();
        if (blockedProtocols == null || blockedProtocols.isEmpty()) {
            return false;
        }
        return blockedProtocols.contains(playerProtocolVersion);
    }

    public String getPlayerVersion(final Player player) {
        try {
            final int protocolVersion = Via.getAPI().getPlayerVersion(player.getUniqueId());
            return String.valueOf(protocolVersion);
        } catch (final Exception e) {
            return "Unknown";
        }
    }
}
