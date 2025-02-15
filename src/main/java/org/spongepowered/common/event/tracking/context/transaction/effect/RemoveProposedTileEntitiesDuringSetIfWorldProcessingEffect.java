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
package org.spongepowered.common.event.tracking.context.transaction.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.accessor.world.level.LevelAccessor;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.BlockPipeline;
import org.spongepowered.common.event.tracking.context.transaction.pipeline.PipelineCursor;
import org.spongepowered.common.world.SpongeBlockChangeFlag;

import java.util.Iterator;

public final class RemoveProposedTileEntitiesDuringSetIfWorldProcessingEffect implements ProcessingSideEffect {

    private static final class Holder {
        static final RemoveProposedTileEntitiesDuringSetIfWorldProcessingEffect INSTANCE = new RemoveProposedTileEntitiesDuringSetIfWorldProcessingEffect();
    }

    public static RemoveProposedTileEntitiesDuringSetIfWorldProcessingEffect getInstance() {
        return Holder.INSTANCE;
    }

    RemoveProposedTileEntitiesDuringSetIfWorldProcessingEffect() {}

    @Override
    public EffectResult processSideEffect(final BlockPipeline pipeline, final PipelineCursor oldState, final BlockState newState,
        final SpongeBlockChangeFlag flag,
        final int limit
    ) {
        final ServerLevel serverWorld = pipeline.getServerWorld();
        final @Nullable BlockEntity tileEntity = oldState.tileEntity;
        final BlockPos pos = oldState.pos;
        if (tileEntity == null || tileEntity.isRemoved()) {
            return EffectResult.NULL_RETURN;
        }
        if (((LevelAccessor) serverWorld).accessor$updatingBlockEntities()) {
            final Iterator<BlockEntity> iterator = ((LevelAccessor) serverWorld).accessor$pendingBlockEntities().iterator();

            while(iterator.hasNext()) {
                final BlockEntity tileentity = iterator.next();
                if (tileentity.getBlockPos().equals(pos)) {
                    tileentity.setRemoved();
                    iterator.remove();
                }
            }
            serverWorld.blockEntityList.add(tileEntity);
            return EffectResult.NULL_RETURN;
        }
        return EffectResult.NULL_PASS;
    }
}
