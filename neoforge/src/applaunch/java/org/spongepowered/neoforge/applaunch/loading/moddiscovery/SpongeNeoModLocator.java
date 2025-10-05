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
package org.spongepowered.neoforge.applaunch.loading.moddiscovery;

import com.google.common.collect.ImmutableMap;
import net.neoforged.fml.classloading.SecureJar;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.neoforge.applaunch.plugin.NeoForgePluginPlatform;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class SpongeNeoModLocator implements IModFileCandidateLocator {
    private static final Logger LOGGER = LogManager.getLogger();

    public SpongeNeoModLocator() {
        NeoForgePluginPlatform.bootstrap();
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        final ModFileDiscoveryAttributes attributes = ModFileDiscoveryAttributes.DEFAULT.withLocator(this);

        if (!FMLEnvironment.isProduction()) {
            final String resourcesProp = System.getProperty("sponge.resources");
            if (resourcesProp != null) {
                for (final String entry : resourcesProp.split(File.pathSeparator)) {
                    if (entry.isBlank()) {
                        continue;
                    }

                    final JarContents jarContents;
                    try {
                        jarContents = JarContents.ofPaths(Stream.of(entry.split("&")).map(Path::of).toList());
                    } catch (IOException e) {
                        LOGGER.error("Failed to create jar contents from {}", entry, e);
                        continue;
                    }

                    IModFile modFile = null;
                    try {
                        // attempt to load as mod or plugin
                        modFile = pipeline.readModFile(jarContents, attributes);
                    } catch (Exception ignored) {}
                    if (modFile == null) {
                        // fallback to game library
                        modFile = IModFile.create(SecureJar.from(jarContents), JarModsDotTomlModFileReader::manifestParser, IModFile.Type.GAMELIBRARY, attributes);
                    }
                    pipeline.addModFile(modFile);
                }
            }
            return;
        }

        try {
            URL rootJar = SpongeNeoModLocator.class.getProtectionDomain().getCodeSource().getLocation();
            FileSystem fs =  FileSystems.getFileSystem(rootJar.toURI()); // FML has already opened a file system for this jar
            try (Stream<Path> st = Files.list(fs.getPath("jars"))) {
                st.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .map(path -> {
                        try {
                            URI jij = new URI("jij:" + path.toAbsolutePath().toUri().getRawSchemeSpecificPart()).normalize();
                            final Map<String, ?> env = ImmutableMap.of("packagePath", path);
                            FileSystem jijFS = FileSystems.newFileSystem(jij, env);
                            return jijFS.getPath("/"); // root of the archive to load
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).forEach(path -> pipeline.addPath(path, attributes, IncompatibleFileReporting.WARN_ALWAYS));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to scan mod candidates", e);
        }
    }

    @Override
    public String toString() {
        return "spongeneo";
    }
}
