package com.practice.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Party (grupo de jugadores) para jugar duelos en equipo.
 */
public final class Party {

    private final UUID id;
    private UUID owner;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> invites = new HashSetWrapper();
    private final long createdAt;
    private boolean open = false;

    public Party(UUID owner) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.members.add(owner);
        this.createdAt = System.currentTimeMillis();
    }

    public UUID id() {
        return this.id;
    }

    public UUID owner() {
        return this.owner;
    }

    public void owner(UUID value) {
        this.owner = value;
    }

    public Set<UUID> members() {
        return Collections.unmodifiableSet(this.members);
    }

    public Set<UUID> rawMembers() {
        return this.members;
    }

    public Set<UUID> invites() {
        return this.invites;
    }

    public int size() {
        return this.members.size();
    }

    public boolean isOwner(UUID uuid) {
        return this.owner.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return this.members.contains(uuid);
    }

    public boolean invite(UUID uuid) {
        if (this.members.contains(uuid)) {
            return false;
        }
        return this.invites.add(uuid);
    }

    public void removeInvite(UUID uuid) {
        this.invites.remove(uuid);
    }

    public void add(UUID uuid) {
        this.members.add(uuid);
        this.invites.remove(uuid);
    }

    public void remove(UUID uuid) {
        this.members.remove(uuid);
        if (this.owner.equals(uuid) && !this.members.isEmpty()) {
            this.owner = this.members.iterator().next();
        }
    }

    public boolean empty() {
        return this.members.isEmpty();
    }

    public long createdAt() {
        return this.createdAt;
    }

    public boolean open() {
        return this.open;
    }

    public void open(boolean value) {
        this.open = value;
    }

    /** Envoltorio publico para mantener el encapsulado del conjunto. */
    private static final class HashSetWrapper extends java.util.LinkedHashSet<UUID> {
        private static final long serialVersionUID = 1L;
    }
}
