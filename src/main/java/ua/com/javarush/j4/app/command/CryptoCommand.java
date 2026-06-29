package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.error.UnreadableSourceException;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;

/** Template Method: read → transform → name → write. Subclasses supply the two varying steps. */
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
            text = readers.pick(input).read(input);
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
