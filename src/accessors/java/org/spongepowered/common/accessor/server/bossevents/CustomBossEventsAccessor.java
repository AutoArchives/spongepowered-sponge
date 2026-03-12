package org.spongepowered.common.accessor.server.bossevents;

import com.mojang.serialization.Codec;
import net.minecraft.server.bossevents.CustomBossEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.common.UntransformedAccessorError;

@Mixin(CustomBossEvents.class)
public interface CustomBossEventsAccessor {
    @Accessor("CODEC") static Codec<CustomBossEvents> accessor$codec() {
        throw new UntransformedAccessorError();
    }
}
