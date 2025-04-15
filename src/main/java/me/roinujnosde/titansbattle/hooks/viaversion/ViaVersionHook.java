package me.roinujnosde.titansbattle.hooks.viaversion;

import com.viaversion.viaversion.api.Via;
import me.roinujnosde.titansbattle.BaseGameConfiguration;
import me.roinujnosde.titansbattle.TitansBattle;
import org.bukkit.entity.Player;

import java.util.List;

public class ViaVersionHook {
    private final List<Integer> blockedProtocols;

    public ViaVersionHook(TitansBattle plugin) {
        this.blockedProtocols = plugin.getConfigManager().getBlockedProtocols();
    }

    /**
     * Verifica se a versão de protocolo do jogador está contida na lista de protocolos bloqueados.
     *
     * @param player o jogador que será checado
     * @return true se a versão do protocolo do jogador estiver na lista blockedProtocols; caso contrário, false.
     */
    public boolean isPlayerVersionBlocked(Player player) {
        int playerProtocolVersion = Via.getAPI().getPlayerVersion(player.getUniqueId());
        return blockedProtocols.contains(playerProtocolVersion);
    }

    /**
     * Verifica se a versão de protocolo do jogador está contida na lista de protocolos bloqueados.
     *
     * @param player o jogador que será checado
     * @param config a configuração do jogo
     * @return true se a versão do protocolo do jogador estiver na lista blockedProtocols; caso contrário, false.
     */
    public boolean isPlayerVersionBlocked(Player player, BaseGameConfiguration config) {
        int playerProtocolVersion = Via.getAPI().getPlayerVersion(player.getUniqueId());
        return config.getBlockedVersions().contains(playerProtocolVersion);
    }
}
