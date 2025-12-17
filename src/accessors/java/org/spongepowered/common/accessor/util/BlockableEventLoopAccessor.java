package org.spongepowered.common.accessor.util;

import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockableEventLoop.class)
public interface BlockableEventLoopAccessor {

    @Invoker("pollTask") boolean invoker$pollTask();

}
