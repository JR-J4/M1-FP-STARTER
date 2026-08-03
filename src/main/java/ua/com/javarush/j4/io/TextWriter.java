package ua.com.javarush.j4.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes UTF-8 text content to a file. */
// ── SOLID ▸ S — Принцип єдиного обов'язку (SRP) ──
// Клас робить рівно одну річ — записує текст у файл. Порівняйте з TextReader/
// OutputNaming: читання, іменування та запис навмисно розділені на три класи,
// щоб кожен мав власну, окрему причину для зміни.
public final class TextWriter {
    public void write(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
