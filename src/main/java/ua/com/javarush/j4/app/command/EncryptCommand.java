package ua.com.javarush.j4.app.command;

import ua.com.javarush.j4.cipher.Cipher;
import ua.com.javarush.j4.io.OutputNaming;
import ua.com.javarush.j4.io.TextReaders;
import ua.com.javarush.j4.io.TextWriter;

import java.nio.file.Path;

public final class EncryptCommand extends CryptoCommand {
    private final Cipher cipher;

    public EncryptCommand(Path input, Cipher cipher, TextReaders readers, TextWriter writer, OutputNaming naming) {
        super(input, readers, writer, naming);
        this.cipher = cipher;
    }

    @Override
    protected String transform(String text) {
        return cipher.encrypt(text);
    }

    @Override
    protected Path outputPath(OutputNaming naming, Path input) {
        return naming.forEncrypt(input);
    }
}
