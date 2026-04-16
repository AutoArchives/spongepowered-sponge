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
package org.spongepowered.vanilla.generator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.tinylog.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Updates the register method of the {@code BlockStateDataProvider} in the Sponge implementation
 * source set with the current set of {@link BlockState} properties.
 */
public class BlockStateDataProviderGenerator implements Generator {

    @Override
    public String name() {
        return "block state data provider";
    }

    @Override
    public void generate(Context ctx) throws IOException {
        final var cu = ctx.implCompilationUnit("data.provider.block.state", "BlockStateDataProvider");
        final var primaryType = cu.getPrimaryType()
            .orElseThrow(() -> new IllegalStateException("Could not find primary type in BlockStateDataProvider"));

        final var registerMethod = primaryType.getMembers().stream()
            .filter(m -> m instanceof MethodDeclaration)
            .map(m -> (MethodDeclaration) m)
            .filter(m -> m.getNameAsString().equals("register"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Could not find register method in BlockStateDataProvider"));

        final var body = registerMethod.getBody()
            .orElseThrow(() -> new IllegalStateException("register method has no body"));

        // Collect all non-registerProperty statements (e.g. the trailing registrator.asImmutable block)
        final var trailingStatements = body.getStatements().stream()
            .filter(stmt -> {
                if (stmt instanceof ExpressionStmt es) {
                    return !es.toString().contains("registerProperty");
                }
                return true;
            })
            .toList();

        // Rebuild the body: registerProperty calls first, then trailing statements
        final var newBody = new BlockStmt();
        for (final String property : this.vanillaProperties()) {
            newBody.addStatement(StaticJavaParser.parseStatement(
                "BlockStateDataProvider.registerProperty(registrator, BlockStateKeys." + property + ", BlockStateProperties." + property + ");"
            ));
        }
        for (final var stmt : trailingStatements) {
            newBody.addStatement(stmt.clone());
        }

        registerMethod.setBody(newBody);

        Logger.info("Updated BlockStateDataProvider.register() with {} property registrations", this.vanillaProperties().size());
    }

    private Set<String> vanillaProperties() {
        return Arrays.stream(BlockStateProperties.class.getDeclaredFields())
                .filter(f -> Property.class.isAssignableFrom(f.getType()))
                .map(Field::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
