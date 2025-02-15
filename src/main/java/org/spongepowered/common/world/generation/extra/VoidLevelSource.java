package org.spongepowered.common.world.generation.extra;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.common.util.Constants;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VoidLevelSource extends ChunkGenerator {
    public static final MapCodec<VoidLevelSource> CODEC = RecordCodecBuilder.mapCodec(
        (instance) -> instance
            .group(
                BiomeSource.CODEC.fieldOf("biome_source")
                    .forGetter((arg) -> arg.biomeSource),
                NoiseGeneratorSettings.CODEC.fieldOf("settings")
                    .forGetter((arg) -> arg.settings)
            )
            .apply(instance, instance.stable(VoidLevelSource::new))
    );
    private Holder<NoiseGeneratorSettings> settings;
    protected static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public VoidLevelSource(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> noiseGeneratorSettingsHolder) {
        super(biomeSource);
        this.settings = noiseGeneratorSettingsHolder;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion var1, long var2, RandomState var4, BiomeManager var5, StructureManager var6, ChunkAccess var7, GenerationStep.Carving var8) {

    }

    @Override
    public void buildSurface(WorldGenRegion var1, StructureManager var2, RandomState var3, ChunkAccess var4) {

    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion var1) {

    }

    @Override
    public int getGenDepth() {
        return this.settings.value().noiseSettings().height();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender arg1, RandomState arg2, StructureManager arg3, ChunkAccess access) {
        return CompletableFuture.completedFuture(access);
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor accessor, RandomState var5) {
        return accessor.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int $$0, int $$1, LevelHeightAccessor accessor, RandomState $$3) {
        final var column = new BlockState[accessor.getHeight()];
        for (int i = 0; i < accessor.getHeight(); ++i) {
            column[i] = AIR;
        }
        return new NoiseColumn(
            accessor.getMinBuildHeight(),
            column
        );
    }

    @Override
    public void addDebugScreenInfo(List<String> var1, RandomState var2, BlockPos var3) {

    }
}
