package me.roinujnosde.titansbattle.hooks.viaversion;

import com.viaversion.viaversion.api.Via;
import me.roinujnosde.titansbattle.BaseGameConfiguration;
import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ViaVersionHook {
    private final List<Integer> blockedProtocols;

    public ViaVersionHook(final @NotNull TitansBattle plugin) {
        this.blockedProtocols = plugin.getConfigManager().getBlockedProtocols();
    }

    /**
     * Verifica se a versão de protocolo do jogador está contida na lista de protocolos bloqueados.
     *
     * @param player o jogador que será verificado
     * @return true se a versão do protocolo do jogador estiver na lista blockedProtocols; caso contrário, false.
     */
    public boolean isPlayerVersionBlocked(final @NotNull Player player) {
        final int playerProtocolVersion = Via.getAPI().getPlayerVersion(player.getUniqueId());
        return blockedProtocols.contains(playerProtocolVersion);
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
        return config.getBlockedProtocols().contains(playerProtocolVersion);
    }
}
