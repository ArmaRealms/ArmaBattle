package me.roinujnosde.titansbattle.hooks.viaversion;

import com.viaversion.viaversion.api.Via;
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
        // Obtém a versão de protocolo do jogador através da API do ViaVersion
        int playerProtocolVersion = Via.getAPI().getPlayerVersion(player.getUniqueId());
        // Retorna true se a versão do jogador estiver na lista de protocolos bloqueados, caso contrário false
        return blockedProtocols.contains(playerProtocolVersion);
    }
}
