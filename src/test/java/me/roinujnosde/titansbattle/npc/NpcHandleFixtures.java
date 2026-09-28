package me.roinujnosde.titansbattle.npc;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public final class NpcHandleFixtures {
    private NpcHandleFixtures() {
    }

    public static NpcHandle vanillaHandle(final UUID ownerId, final LivingEntity mob) {
        return new VanillaProvider.VanillaNpcHandle(ownerId, mob);
    }
}
