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
package org.spongepowered.common.test.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.leangen.geantyref.TypeToken;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.Rotation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.EventListenerRegistration;
import org.spongepowered.api.event.Order;
import org.spongepowered.common.launch.Launch;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Collects every gametest visible to the running platform: the {@link GameTest}
 * methods of every {@link GameTestGroup} class found in a plugin, whether Sponge's
 * own or a plugin's, with nothing to declare beyond the annotation.
 */
public final class SpongeGameTests {

    private static final Pattern WORD_START = Pattern.compile("(?<=[a-z0-9])(?=[A-Z])");
    // 1.21.1 ships no empty test structure, so the default is ours, served from the Sponge plugin's own resources
    private static final String EMPTY = "minecraft:empty";
    private static final String SPONGE_EMPTY = "sponge:empty";
    private static final String GROUP = Type.getDescriptor(GameTestGroup.class);
    private static final String METADATA = "META-INF/sponge_plugins.json";
    // Each scanned test class and the plugin it came with, when its resource declares exactly one
    private static final Map<Class<?>, String> OWNERS = new HashMap<>();

    public static List<TestFunction> all() {
        return Holder.ALL;
    }

    public static boolean isTest(final Method method) {
        return method.isAnnotationPresent(GameTest.class);
    }

    /**
     * {@code <group>.<method_name>}, e.g. {@code piston.push_row} for {@code PistonGameTests#pushRow}. Test functions
     * are named rather than keyed before 1.21.5, and {@code /test} only takes unquoted words, so this is the
     * {@code sponge:<group>/<method_name>} id of later versions in the form vanilla names its own tests.
     */
    public static String name(final Method method) {
        return SpongeGameTests.group(method) + "." + SpongeGameTests.WORD_START.matcher(method.getName()).replaceAll("_").toLowerCase();
    }

    /** Runs every test of the class with {@code /test runall <group>}; a plugin's groups carry its id */
    public static String group(final Method method) {
        final Class<?> type = method.getDeclaringClass();
        final GameTestGroup group = type.getAnnotation(GameTestGroup.class);
        if (group == null) {
            throw new IllegalStateException(type.getName() + " has gametests but no @GameTestGroup");
        }
        final String plugin = group.plugin().isEmpty() ? SpongeGameTests.OWNERS.getOrDefault(type, "") : group.plugin();
        return plugin.isEmpty() ? group.value() : plugin + "." + group.value();
    }

    public static <E extends Event> void listen(final Class<E> type, final EventListener<? super E> listener) {
        SpongeGameTests.listen(TypeToken.get(type), Order.DEFAULT, listener);
    }

    // ponytail: a test that times out never reaches its unregister; harmless since listeners filter by position
    // and the gametest server exits afterwards. Unregister from a GameTestListener if tests ever share a long-lived server.
    public static <E extends Event> void listen(final TypeToken<E> type, final Order order, final EventListener<? super E> listener) {
        Sponge.eventManager().registerListener(EventListenerRegistration.builder(type)
            .plugin(Launch.instance().commonPlugin())
            .order(order)
            .listener(listener)
            .build());
    }

    private static Stream<TestFunction> tests(final Class<?> type) {
        final Object instance;
        try {
            final Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Gametest class " + type.getName() + " needs a no-argument constructor", e);
        }
        return Arrays.stream(type.getDeclaredMethods()).filter(SpongeGameTests::isTest).map(method -> SpongeGameTests.test(instance, method));
    }

    // Instance methods rather than static: IntelliJ only gives non-static methods a run icon
    private static TestFunction test(final Object instance, final Method method) {
        if (Modifier.isStatic(method.getModifiers()) || !Arrays.equals(method.getParameterTypes(), new Class<?>[] {GameTestHelper.class})) {
            throw new IllegalStateException("@GameTest " + method + " must not be static, and must take only a GameTestHelper");
        }
        final GameTest test = method.getAnnotation(GameTest.class);
        final String structure = SpongeGameTests.EMPTY.equals(test.structure()) ? SpongeGameTests.SPONGE_EMPTY : test.structure();
        return new TestFunction(SpongeGameTests.group(method), SpongeGameTests.name(method), structure, Rotation.NONE, test.maxTicks(),
            test.setupTicks(), test.required(), false, 1, 1, test.skyAccess(), SpongeGameTests.function(instance, method));
    }

    private static Consumer<GameTestHelper> function(final Object instance, final Method method) {
        final MethodHandle handle;
        try {
            method.setAccessible(true);
            handle = MethodHandles.lookup().unreflect(method).bindTo(instance);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return helper -> {
            try {
                handle.invoke(helper);
            } catch (final RuntimeException | Error e) {
                throw e;
            } catch (final Throwable t) {
                throw new RuntimeException(t);
            }
        };
    }

    // Resolved once, when GameTestRegistry is first loaded; every plugin is in the game layer by then
    private static final class Holder {
        static final List<TestFunction> ALL = SpongeGameTests.scan().stream().flatMap(SpongeGameTests::tests).toList();
    }

    /**
     * Every {@link GameTestGroup} class of every plugin, read with ASM so that only test classes are loaded. A plugin's
     * tests reach the game with it, as a {@code -gametests} jar or the test output alongside its own.
     */
    private static List<Class<?>> scan() {
        // Every plugin resource in the game layer, by its metadata rather than its containers: one loader serves the whole
        // layer, so a container's locateResource answers with whichever resource comes first
        final Map<Path, List<String>> roots = new LinkedHashMap<>();
        try {
            for (final URL metadata : Collections.list(SpongeGameTests.class.getClassLoader().getResources(SpongeGameTests.METADATA))) {
                final Path path = Path.of(metadata.toURI());
                roots.put(path.getParent().getParent(), SpongeGameTests.declaredPlugins(path));
            }
        } catch (final IOException | URISyntaxException e) {
            throw new IllegalStateException("Listing plugin metadata for gametests", e);
        }
        final List<Class<?>> found = new ArrayList<>();
        for (final Map.Entry<Path, List<String>> entry : roots.entrySet()) {
            final Path root = entry.getKey();
            final @Nullable String owner = entry.getValue().size() == 1 ? entry.getValue().get(0) : null;
            try (final Stream<Path> files = Files.walk(root)) {
                for (final Path file : files.filter(file -> file.toString().endsWith(".class")).toList()) {
                    final String name = root.relativize(file).toString().replace(root.getFileSystem().getSeparator(), "/");
                    if (name.startsWith("META-INF/") || name.equals("module-info.class")) {
                        continue;
                    }
                    try (final InputStream in = Files.newInputStream(file)) {
                        if (SpongeGameTests.isGroup(in)) {
                            // One loader serves the whole game layer, so this one reaches every plugin's classes
                            final Class<?> type = Class.forName(name.substring(0, name.length() - ".class".length()).replace('/', '.'), false, SpongeGameTests.class.getClassLoader());
                            if (owner != null) {
                                SpongeGameTests.OWNERS.put(type, owner);
                            }
                            found.add(type);
                        }
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException("Scanning " + root + " for gametests", e);
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return found;
    }

    /** The ids in a {@code sponge_plugins.json}: one for a plugin's jar, several for a platform's */
    private static List<String> declaredPlugins(final Path metadata) throws IOException {
        try (final Reader reader = Files.newBufferedReader(metadata)) {
            final List<String> ids = new ArrayList<>();
            for (final JsonElement plugin : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("plugins")) {
                ids.add(plugin.getAsJsonObject().get("id").getAsString());
            }
            return ids;
        }
    }

    private static boolean isGroup(final InputStream in) throws IOException {
        final boolean[] group = new boolean[1];
        new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
                group[0] |= SpongeGameTests.GROUP.equals(descriptor);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return group[0];
    }

    private SpongeGameTests() {
    }
}
