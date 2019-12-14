/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.api.common.entity.player;

import static com.google.common.base.Preconditions.checkNotNull;

import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.world.storage.WorldInfoBridge;
import org.spongepowered.common.entity.player.SpongeUser;
import org.spongepowered.math.vector.Vector3d;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = SpongeUser.class, remap = false)
public abstract class SpongeUserMixin_API implements User {

    @Shadow @Final private com.mojang.authlib.GameProfile profile;

    @Shadow private double posX;
    @Shadow private double posY;
    @Shadow private double posZ;
    @Shadow private int dimension;
    @Shadow private float rotationPitch;
    @Shadow private float rotationYaw;
    @Shadow public abstract void shadow$markDirty();

    @Override
    public GameProfile getProfile() {
        return (GameProfile) this.profile;
    }

    @Override
    public boolean isOnline() {
        return this.getPlayer().isPresent();
    }

    @Override
    public Optional<Player> getPlayer() {
        return Optional.ofNullable((Player) SpongeImpl.getServer().getPlayerList().getPlayerByUUID(this.profile.getId()));
    }

    @Override
    public boolean validateRawData(DataView container) {
        throw new UnsupportedOperationException(); // TODO Data API
    }

    @Override
    public void setRawData(DataView container) throws InvalidDataException {
        throw new UnsupportedOperationException(); // TODO Data API
    }

    @Override
    public Vector3d getPosition() {
        return this.getPlayer()
            .map(User::getPosition)
            .orElseGet(() -> new Vector3d(this.posX, this.posY, this.posZ));
    }

    @Override
    public Optional<UUID> getWorldUniqueId() {
        final Optional<Player> playerOpt = this.getPlayer();
        if (playerOpt.isPresent()) {
            return playerOpt.get().getWorldUniqueId();
        }
        final DimensionType dimensionType = DimensionType.getById(this.dimension);
        if (dimensionType == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(SpongeImpl.getWorldManager().getDimensionTypeUniqueId(dimensionType));
    }

    @Override
    public boolean setLocation(Vector3d position, UUID worldUniqueId) {
        final Optional<Player> playerOpt = this.getPlayer();
        if (playerOpt.isPresent()) {
            return playerOpt.get().setLocation(position, worldUniqueId);
        }
        final WorldProperties properties =
                SpongeImpl.getWorldManager().getProperties(worldUniqueId).orElseThrow(() -> new IllegalArgumentException(String.format("Unknown "
                        + "World UUID '%s' given when setting location of user!", worldUniqueId)));
        final Integer dimensionId = ((WorldInfoBridge) properties).bridge$getDimensionType().getId();
        this.dimension = dimensionId;
        this.posX = position.getX();
        this.posY = position.getY();
        this.posZ = position.getZ();
        this.shadow$markDirty();
        return true;
    }

    @Override
    public Vector3d getRotation() {
        return this.getPlayer()
            .map(Entity::getRotation)
            .orElseGet(() -> new Vector3d(this.rotationPitch, this.rotationYaw, 0));
    }

    @Override
    public void setRotation(final Vector3d rotation) {
        checkNotNull(rotation, "Rotation was null!");
        final Optional<Player> playerOpt = this.getPlayer();
        if (playerOpt.isPresent()) {
            playerOpt.get().setRotation(rotation);
            return;
        }
        this.shadow$markDirty();
        this.rotationPitch = ((float) rotation.getX()) % 360.0F;
        this.rotationYaw = ((float) rotation.getY()) % 360.0F;
    }

    @Override
    public String getIdentifier() {
        return this.profile.getId().toString();
    }
}
