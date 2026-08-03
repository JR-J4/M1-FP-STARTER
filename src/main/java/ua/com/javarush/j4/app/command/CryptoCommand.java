package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.error.UnreadableSourceException;
import ua.com.javarush.j4.io.*;

import java.io.IOException;
import java.nio.file.Path;

/** Template Method: read → transform → name → write. Subclasses supply the two varying steps. */
// ── SOLID ▸ L — Принцип підстановки Лісков (LSP) ──
// EncryptCommand, DecryptCommand і BruteForceCommand повністю взаємозамінні:
// CryptoService викликає execute() через тип CryptoCommand і НЕ перевіряє, який
// саме підклас перед ним. Кожен підклас чесно виконує контракт «прочитати →
// перетворити → записати», лише підставляючи власні transform()/outputPath().
// Жоден підклас не звужує поведінку й не кидає несподіваних винятків — це LSP.
public abstract class CryptoCommand {
    private final Path input;
    private final TextReaders readers;
    private final TextWriter writer;
    private final OutputNaming naming;

    protected CryptoCommand(Path input, TextReaders readers, TextWriter writer, OutputNaming naming) {
        this.input = input;
        this.readers = readers;
        this.writer = writer;
        this.naming = naming;
    }

    public final Path execute() throws IOException {
        String text;
        try {
            TextReader pick = readers.pick(input);
            pick.getMeta();
            text = pick.read(input);
        } catch (IOException e) {
            throw new UnreadableSourceException("Cannot read input file: " + input, e);
        }
        String result = transform(text);
        Path output = outputPath(naming, input);
        writer.write(output, result);
        return output;
    }

    protected abstract String transform(String text);

    protected abstract Path outputPath(OutputNaming naming, Path input);
}
