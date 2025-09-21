package me.roinujnosde.titansbattle;

import me.roinujnosde.titansbattle.types.Group;
import me.roinujnosde.titansbattle.types.GroupData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FakeGroup extends Group {

    private static int lastId = 0;
    private final int id;
    private final List<UUID> members = new ArrayList<>();

    public FakeGroup(@NotNull final GroupData data) {
        super(data);
        id = ++lastId;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        final FakeGroup fakeGroup = (FakeGroup) object;
        return getId() == fakeGroup.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }

    @Override
    public @NotNull String getName() {
        return Integer.toString(id);
    }

    @Override
    public @NotNull String getId() {
        return Integer.toString(id);
    }

    @Override
    public void disband() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMember(@NotNull final UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(@NotNull final UUID uuid) {
        members.add(uuid);
    }

    @Override
    public boolean isLeaderOrOfficer(@NotNull final UUID uuid) {
        return members.contains(uuid) && members.getFirst() == uuid;
    }
}
