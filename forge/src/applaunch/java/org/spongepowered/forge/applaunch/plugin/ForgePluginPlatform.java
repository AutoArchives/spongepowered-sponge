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
package org.spongepowered.forge.applaunch.plugin;

import cpw.mods.modlauncher.Environment;
import cpw.mods.modlauncher.api.IEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.spongepowered.common.applaunch.AppLaunch;
import org.spongepowered.common.applaunch.config.LaunchConfig;
import org.spongepowered.common.applaunch.config.TokenReplacement;
import org.spongepowered.common.applaunch.plugin.PluginPlatform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ForgePluginPlatform implements PluginPlatform {
    private static final List<AutoCloseable> closeCallbacks = new ArrayList<>();

    private static volatile boolean bootstrapped;

    private final Environment environment;
    private final LaunchConfig config;
    private final TokenReplacement tokens;
    private final List<Path> pluginDirectories;

    public static synchronized void bootstrap(final Environment environment) {
        if (ForgePluginPlatform.bootstrapped) {
            return;
        }
        ForgePluginPlatform.bootstrapped = true;
        final ForgePluginPlatform platform;
        try {
            platform = new ForgePluginPlatform(environment);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        AppLaunch.setPluginPlatform(platform);
    }

    private ForgePluginPlatform(final Environment environment) throws IOException {
        this.environment = environment;
        this.config = LaunchConfig.load(this.baseDirectory(), true);

        this.tokens = new TokenReplacement();
        this.tokens.register("BASE_DIR", this.baseDirectory());
        this.tokens.register("CONFIG_DIR", this.configDirectory());
        this.tokens.register("MODS_DIR", FMLPaths.MODSDIR.get());

        final Path additionalPluginsDirectory = Path.of(this.tokens.replace(this.config.additionalPluginsDirectory()));
        Files.createDirectories(additionalPluginsDirectory);
        this.pluginDirectories = List.of(FMLPaths.MODSDIR.get(), additionalPluginsDirectory);
    }

    @Override
    public String version() {
        return this.environment.getProperty(IEnvironment.Keys.VERSION.get()).orElse("dev");
    }

    @Override
    public boolean vanilla() {
        return false;
    }

    @Override
    public Path baseDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public LaunchConfig config() {
        return this.config;
    }

    @Override
    public TokenReplacement tokens() {
        return this.tokens;
    }

    @Override
    public List<Path> pluginDirectories() {
        return this.pluginDirectories;
    }

    @Override
    public void addLoaderCloseCallback(final AutoCloseable closeable) {
        ForgePluginPlatform.closeCallbacks.add(closeable);
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (final AutoCloseable closeCallback : ForgePluginPlatform.closeCallbacks) {
                try {
                    closeCallback.close();
                } catch (Exception e) {
                    PluginPlatform.LOGGER.error("Failed to run close callback {}", closeCallback, e);
                }
            }
        }, "Sponge shutdown thread"));
    }
}
