package ua.com.javarush.j4.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OutputNamingTest {

    private final OutputNaming naming = new OutputNaming();

    @Test
    void encryptInsertsMarkerBeforeExtension() {
        assertEquals("plain [ENCRYPTED].txt",
                naming.forEncrypt(Path.of("plain.txt")).getFileName().toString());
    }

    @Test
    void decryptReplacesEncryptedMarker() {
        String out = naming.forDecrypt(Path.of("plain [ENCRYPTED].txt")).getFileName().toString();
        assertTrue(out.contains("[DECRYPTED]"));
        assertFalse(out.contains("[ENCRYPTED]"));
    }

    @Test
    void decryptInsertsMarkerWhenNoEncryptedMarkerPresent() {
        assertEquals("plain [DECRYPTED].txt",
                naming.forDecrypt(Path.of("plain.txt")).getFileName().toString());
    }

    @Test
    void preservesParentDirectory() {
        Path out = naming.forEncrypt(Path.of("/tmp/sub/plain.txt"));
        assertEquals(Path.of("/tmp/sub"), out.getParent());
    }
}
