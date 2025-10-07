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
package org.spongepowered.gradle.impl;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DigestUtil {
    private static final char[] hexChars = "0123456789abcdef".toCharArray();

    public static String toHexString(final byte[] bytes) {
        final char[] chars = new char[bytes.length << 1];
        int i = 0;
        for (final byte b : bytes) {
            chars[i++] = DigestUtil.hexChars[(b >> 4) & 15];
            chars[i++] = DigestUtil.hexChars[b & 15];
        }
        return new String(chars);
    }

    public static String digest(final Path file, final String algorithm) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new GradleException("Failed to find digest algorithm", e);
        }

        try (final InputStream in = Files.newInputStream(file)) {
            final byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) != -1) {
                digest.update(buf, 0, read);
            }
        } catch (final IOException e) {
            throw new GradleException("Failed to digest file " + file, e);
        }

        return DigestUtil.toHexString(digest.digest());
    }

    public static void generateChecksums(final File jarFile, final String dirPath, final String algorithm) throws Exception {
        try (final FileSystem fs = FileSystems.newFileSystem(new URI("jar:" + jarFile.toURI()), Map.of())) {
            final Path dir = fs.getPath(dirPath);
            final List<String> lines;
            try (final Stream<Path> st = Files.list(dir)) {
                lines = st.map(p -> DigestUtil.digest(p, algorithm) + " " + p.getFileName()).toList();
            }
            Files.write(dir.resolve("checksums.txt"), lines);
        }
    }
}
