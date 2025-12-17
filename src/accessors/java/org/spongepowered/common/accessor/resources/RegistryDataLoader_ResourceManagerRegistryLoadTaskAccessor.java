package org.spongepowered.common.accessor.resources;

import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net/minecraft/resources/RegistryDataLoader$ResourceManagerRegistryLoadTask")
public interface RegistryDataLoader_ResourceManagerRegistryLoadTaskAccessor {

    @Accessor("resourceManager") ResourceManager accessor$getResourceManager();
}
