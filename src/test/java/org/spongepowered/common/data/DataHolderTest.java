package data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.BlockEntityArchetype;
import org.spongepowered.api.block.entity.BlockEntityType;
import org.spongepowered.api.block.entity.BlockEntityTypes;
import org.spongepowered.api.data.*;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.DefaultWorldKeys;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.SpongeDataRegistration;
import org.spongepowered.common.util.Constants;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DataHolderTest {

    private static final Key<Value<String>> STRING_KEY = Key.from(ResourceKey.of("test", "string"), String.class);

    static {
        SpongeDataManager.INSTANCE.registerCustomDataRegistration((SpongeDataRegistration) DataRegistration.of(DataHolderTest.STRING_KEY, ItemStack.class, BlockEntity.class, Entity.class, WorldChunk.class, ServerWorldProperties.class));
    }

    public static Stream<Arguments> testEntity() {
        final ServerWorld world = Sponge.server().worldManager().world(DefaultWorldKeys.DEFAULT).get();

        return Stream.of(
            DataHolderTest.testEntityArguments(() -> ItemStack.of(ItemTypes.DIAMOND), c -> ItemStack.builder().fromContainer(c).build()),
            DataHolderTest.testEntityArguments(() -> BlockEntityArchetype.builder().blockEntity(BlockEntityTypes.CHEST).state(BlockTypes.CHEST.get().defaultState()).build(), c -> BlockEntityArchetype.builder().build(c).orElseThrow()),
            DataHolderTest.testEntityArguments(() -> world.createEntity(EntityTypes.CAT, Vector3d.ZERO), c -> world.createEntity(c).orElseThrow())
        );
    }

    private static <T extends SerializableDataHolder.Mutable> Arguments testEntityArguments(final Supplier<T> supplier, final Function<DataContainer, T> function) {
        return Arguments.of(supplier, function);
    }

    @ParameterizedTest
    @MethodSource
    public <T extends SerializableDataHolder.Mutable> void testEntity(final Supplier<T> supplier, final Function<DataContainer, T> function) {
        final var input = "test" + System.currentTimeMillis();

        final T holder = supplier.get();
        final var offerResult = holder.offer(DataHolderTest.STRING_KEY, input);

        Assertions.assertEquals(DataTransactionResult.successResult(Value.immutableOf(DataHolderTest.STRING_KEY, input)), offerResult);
        Assertions.assertEquals(Optional.of(input), holder.get(DataHolderTest.STRING_KEY));

        final DataContainer originalContainer = holder.toContainer();

        final T copy = function.apply(originalContainer);

        final DataContainer copyContainer = copy.toContainer();

        originalContainer.remove(Constants.Sponge.UNSAFE_NBT.then(Constants.Entity.ENTITY_UUID));
        copyContainer.remove(Constants.Sponge.UNSAFE_NBT.then(Constants.Entity.ENTITY_UUID));

        Assertions.assertEquals(originalContainer, copyContainer);
    }
}
