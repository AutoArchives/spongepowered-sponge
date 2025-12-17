package org.spongepowered.common.accessor.resources;

import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RegistryDataLoader.RegistryLoadTask.class)
public interface RegistryDataLoader_RegistryLoadTaskAccessor<E> {

    @Accessor("registry") WritableRegistry<E> accessor$getRegistry();

}
