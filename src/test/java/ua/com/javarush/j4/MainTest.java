package ua.com.javarush.j4;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Інтеграційні тести програми. Кожен тест запускає {@code Main.main(...)}
 * у тимчасовій директорії ({@link TempDir}) і перевіряє створений файл.
 *
 * <p>У підкоментарях до кожного тесту вказано:
 * <ul>
 *   <li><b>Що перевіряє</b> — яка частина функціонала тестується;</li>
 *   <li><b>Як пройти</b> — що саме треба реалізувати, щоб тест став зеленим.</li>
 * </ul>
 *
 * <p>Якщо команда не створює вихідного файлу або {@code Main} кидає виняток,
 * повідомлення про помилку вказує на відповідний крок із {@code CHECKLIST.md},
 * щоб одразу було видно, куди йти далі.
 */
class MainTest {
    private static final String ENCRYPT_COMMAND = "-e";
    private static final String DECRYPT_COMMAND = "-d";
    private static final String BF_COMMAND = "-b";

    private static final String HAMLET_EN = loadResource("hamlet.txt");
    private static final String ORWELL_UA = loadResource("orwell.txt");

    /**
     * Українські тести вимкнено за замовчуванням. Увімкни їх прапорцем
     * {@code -Dtest.lang.ua=true} (напр. {@code ./mvnw test -Dtest.lang.ua=true}).
     */
    private static final boolean UA_ENABLED = Boolean.getBoolean("test.lang.ua");

    private static String loadResource(String resourceName) {
        try (var in = MainTest.class.getResourceAsStream("/" + resourceName)) {
            if (in == null) {
                throw new RuntimeException("Test resource not found on classpath: " + resourceName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test resource: " + resourceName, e);
        }
    }

    @TempDir
    private Path tempDir;

    private Path inputFilePathEN;

    @BeforeEach
    public void setUp() throws IOException {
        inputFilePathEN = createTestFile("EN_Text.txt", HAMLET_EN);
    }

    private Path createTestFile(String fileName, String content) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        Files.writeString(filePath, content);
        return filePath;
    }

    /**
     * Запускає {@code Main.main} з опційно-стильовими аргументами і повертає шлях
     * до нового файлу, який з'явився у {@code tempDir} після виклику. Якщо такого
     * файлу не з'явилось — повідомлення про помилку містить підказку щодо команди
     * і крок із CHECKLIST, у якому ця команда реалізовується.
     */
    private Path execute(String command, Path inputFilePath, int key) {
        return execute(new String[]{command, "-k", String.valueOf(key), "-f", inputFilePath.toString()}, command);
    }

    /**
     * Перебір ключів ({@code -b}) не потребує ключа — за технічним завданням команда
     * сама його підбирає. Тому ця версія формує аргументи без {@code -k}.
     */
    private Path execute(String command, Path inputFilePath) {
        return execute(new String[]{command, "-f", inputFilePath.toString()}, command);
    }

    private Path execute(String[] args, String command) {
        List<Path> filesBefore = listFiles(tempDir);

        try {
            Main.main(args);
        } catch (UnsupportedOperationException uoe) {
            return fail(commandHint(command)
                    + " — реалізація кидає UnsupportedOperationException."
                    + " Допиши цю гілку Main.", uoe);
        } catch (Exception e) {
            return fail(commandHint(command)
                    + " — Main кинув виняток: "
                    + e.getClass().getSimpleName()
                    + ": " + e.getMessage(), e);
        }

        List<Path> filesAfter = listFiles(tempDir);
        return filesAfter.stream()
                .filter(p -> !filesBefore.contains(p))
                .findFirst()
                .orElseGet(() -> fail(commandHint(command)
                        + " — після виконання у tempDir не з'явилось жодного нового файлу."));
    }

    private static String commandHint(String command) {
        return switch (command) {
            case ENCRYPT_COMMAND -> "Команда '-e' (шифрування). Див. CHECKLIST крок 1";
            case DECRYPT_COMMAND -> "Команда '-d' (розшифрування). Див. CHECKLIST крок 2";
            case BF_COMMAND      -> "Команда '-b' (перебір ключів). Див. CHECKLIST крок 3";
            default              -> "Команда '" + command + "'";
        };
    }

    private List<Path> listFiles(Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list files in directory: " + directory, e);
        }
    }

    private String readFile(Path filePath) {
        Assumptions.assumeTrue(Files.exists(filePath), "File does not exist: " + filePath);
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            fail("Failed to read file: " + filePath, e);
            return null;
        }
    }

    @Nested
    @DisplayName("Перевірка створення файлів, маркерів і вмісту")
    class FileTests {

        @Nested
        @DisplayName("ШИФРУВАННЯ → CHECKLIST крок 1")
        class EncryptFileTests {

            /**
             * <b>Що перевіряє:</b> після виклику {@code -e -k 1 -f plain.txt} (де вміст
             * "ABC") у тій самій папці з'являється новий файл, і його вміст дорівнює
             * "BCD". Це найменший випадок шифрування, де результат можна знати точно.
             *
             * <p><b>Як пройти:</b> реалізуй гілку {@code -e}: прочитай файл, зсунь кожну
             * літеру на ключ по колу алфавіту, запиши результат у новий файл.
             */
            @Test
            @DisplayName("Створюється новий файл із зашифрованим вмістом ('ABC' + 1 = 'BCD')")
            void encryptFileCreating() throws IOException {
                Path input = createTestFile("plain.txt", "ABC");

                Path encryptedFile = execute(ENCRYPT_COMMAND, input, 1);

                assertTrue(Files.exists(encryptedFile),
                        "Зашифрований файл не створено.");
                assertEquals("BCD", readFile(encryptedFile),
                        "Вміст зашифрованого файлу не збігається з очікуваним.");
            }

            /**
             * <b>Що перевіряє:</b> ім'я зашифрованого файлу містить мітку {@code [ENCRYPTED]}
             * (наприклад, {@code plain.txt} → {@code plain [ENCRYPTED].txt}).
             *
             * <p><b>Як пройти:</b> при формуванні імені вихідного файлу для {@code -e}
             * додавай {@code [ENCRYPTED]} перед розширенням.
             */
            @Test
            @DisplayName("Ім'я зашифрованого файлу містить мітку '[ENCRYPTED]'")
            void encryptFileMarker() throws IOException {
                Path input = createTestFile("plain.txt", "ABC");

                Path encryptedFile = execute(ENCRYPT_COMMAND, input, 1);

                assertTrue(encryptedFile.getFileName().toString().contains("[ENCRYPTED]"),
                        "Очікувалась мітка '[ENCRYPTED]' в імені файлу: "
                                + encryptedFile.getFileName());
            }

            /**
             * <b>Що перевіряє:</b> порядок аргументів довільний — {@code -f}, {@code -k}
             * і команда {@code -e} у будь-якій послідовності дають той самий результат
             * ({@code "ABC"} + 1 = {@code "BCD"}).
             *
             * <p><b>Як пройти:</b> парсер має визначати команду й опції за самими
             * прапорцями, а не за їхньою позицією.
             */
            @Test
            @DisplayName("Порядок аргументів довільний (-f, -k, -e у будь-якому порядку)")
            void argumentOrderIsArbitrary() throws IOException {
                Path input = createTestFile("plain.txt", "ABC");

                Path encrypted = execute(
                        new String[]{"-f", input.toString(), "-k", "1", ENCRYPT_COMMAND},
                        ENCRYPT_COMMAND);

                assertEquals("BCD", readFile(encrypted),
                        "Програма має приймати аргументи у довільному порядку.");
            }
        }

        @Nested
        @DisplayName("РОЗШИФРУВАННЯ → CHECKLIST крок 2")
        class DecryptFileTests {

            /**
             * <b>Що перевіряє:</b> повний цикл {@code -e} → {@code -d} з тим самим ключем
             * створює файл, у якому вміст дорівнює оригіналу.
             *
             * <p><b>Як пройти:</b> реалізуй гілку {@code -d}. Підказка: розшифрування з
             * ключем {@code k} — це шифрування з ключем {@code -k}.
             */
            @Test
            @DisplayName("Створюється новий файл, де вміст дорівнює оригіналу")
            void decryptedFileCreating() throws IOException {
                String original = "Hello, World!";
                Path input = createTestFile("plain.txt", original);

                Path encrypted = execute(ENCRYPT_COMMAND, input, 5);
                Path decrypted = execute(DECRYPT_COMMAND, encrypted, 5);

                assertTrue(Files.exists(decrypted), "Розшифрований файл не створено.");
                assertEquals(original, readFile(decrypted),
                        "Розшифрований вміст має дорівнювати оригіналу.");
            }

            /**
             * <b>Що перевіряє:</b> ім'я розшифрованого файлу містить мітку {@code [DECRYPTED]}.
             *
             * <p><b>Як пройти:</b> при записі результату {@code -d} формуй ім'я вихідного
             * файлу так, щоб у ньому була мітка {@code [DECRYPTED]}.
             */
            @Test
            @DisplayName("Ім'я розшифрованого файлу містить мітку '[DECRYPTED]'")
            void decryptedFileMarker() throws IOException {
                Path input = createTestFile("plain.txt", "Hello");
                Path encrypted = execute(ENCRYPT_COMMAND, input, 5);

                Path decrypted = execute(DECRYPT_COMMAND, encrypted, 5);

                assertTrue(decrypted.getFileName().toString().contains("[DECRYPTED]"),
                        "Очікувалась мітка '[DECRYPTED]' в імені файлу: "
                                + decrypted.getFileName());
            }
        }

        @Nested
        @DisplayName("ПЕРЕБІР КЛЮЧІВ → CHECKLIST крок 3")
        class BruteForceFileTests {

            /**
             * <b>Що перевіряє:</b> {@code -bf} над зашифрованим англійським текстом
             * створює новий файл, у якому вміст точно дорівнює оригіналу (з урахуванням
             * регістру).
             *
             * <p><b>Як пройти:</b> реалізуй гілку {@code -bf}. Підказка: спробуй кожен
             * правдоподібний ключ, оціни «схожість на справжній текст» (часті слова
             * або частоти літер), вибери варіант з найкращою оцінкою.
             */
            @Test
            @DisplayName("Створюється новий файл з відновленим оригіналом (англ.)")
            void bruteForceFileCreating() {
                Path encrypted = execute(ENCRYPT_COMMAND, inputFilePathEN, 5);

                Path bruteForced = execute(BF_COMMAND, encrypted);

                assertTrue(Files.exists(bruteForced), "Файл після brute-force не створено.");
                assertEquals(HAMLET_EN, readFile(bruteForced),
                        "Brute-force не відновив оригінальний текст.");
            }
        }

        @Nested
        @DisplayName("Перетворення імені файлу при розшифруванні")
        class DecryptFilenameTransformation {

            /**
             * <b>Що перевіряє:</b> при розшифруванні {@code plain [ENCRYPTED].txt} нове ім'я
             * має бути {@code plain [DECRYPTED].txt}, а не {@code plain [ENCRYPTED] [DECRYPTED].txt}.
             * Тобто мітку треба ЗАМІНИТИ, а не додати поряд.
             *
             * <p><b>Як пройти:</b> якщо ім'я вхідного файлу містить {@code [ENCRYPTED]},
             * заміни цю мітку на {@code [DECRYPTED]}; інакше — додай {@code [DECRYPTED]}.
             */
            @Test
            @DisplayName("'[ENCRYPTED]' замінюється на '[DECRYPTED]', а не додається поряд")
            void replacesEncryptedMarker() throws IOException {
                Path input = createTestFile("plain.txt", "Hello");
                Path encrypted = execute(ENCRYPT_COMMAND, input, 5);

                Path decrypted = execute(DECRYPT_COMMAND, encrypted, 5);
                String decryptedName = decrypted.getFileName().toString();

                assertTrue(decryptedName.contains("[DECRYPTED]"),
                        "Очікувалась мітка '[DECRYPTED]' в імені: " + decryptedName);
                assertFalse(decryptedName.contains("[ENCRYPTED]"),
                        "Розшифрований файл не повинен містити мітку '[ENCRYPTED]': "
                                + decryptedName);
            }
        }
    }

    @Nested
    @DisplayName("Тести шифру для двох мов (англійська і українська)")
    class LanguageTests {

        static Stream<Arguments> singleLetterEncryptCases() {
            return Stream.of(
                    Arguments.of("EN", "A", 1, "B"),
                    Arguments.of("EN", "a", 1, "b"),
                    Arguments.of("EN", "A", 25, "Z"),
                    Arguments.of("EN", "a", 25, "z"),
                    Arguments.of("UA", "А", 1, "Б"),
                    Arguments.of("UA", "а", 1, "б"),
                    Arguments.of("UA", "А", 32, "Я"),
                    Arguments.of("UA", "а", 32, "я")
            );
        }

        static Stream<Arguments> singleLetterDecryptCases() {
            return Stream.of(
                    Arguments.of("EN", "B", 1, "A"),
                    Arguments.of("EN", "b", 1, "a"),
                    Arguments.of("EN", "Z", 25, "A"),
                    Arguments.of("EN", "z", 25, "a"),
                    Arguments.of("UA", "Б", 1, "А"),
                    Arguments.of("UA", "б", 1, "а"),
                    Arguments.of("UA", "Я", 32, "А"),
                    Arguments.of("UA", "я", 32, "а")
            );
        }

        static Stream<Arguments> fixtures() {
            return Stream.of(
                    Arguments.of("EN", HAMLET_EN),
                    Arguments.of("UA", ORWELL_UA)
            );
        }

        /**
         * Пропускає українські сценарії, поки не ввімкнено прапорець
         * {@code -Dtest.lang.ua=true}. Англійські сценарії виконуються завжди.
         */
        private void assumeLanguageEnabled(String lang) {
            Assumptions.assumeTrue(!"UA".equals(lang) || UA_ENABLED,
                    "Українські тести вимкнено. Увімкни їх прапорцем -Dtest.lang.ua=true");
        }

        /**
         * <b>Що перевіряє:</b> базовий зсув літер по відповідному алфавіту:
         * A+1=B, a+25=z для англ.; А+1=Б, а+32=я для укр.
         *
         * <p><b>Як пройти:</b> реалізуй шифрування Цезаря, що зсуває літери в межах
         * власного регістру (велика залишається великою, мала — малою). Український
         * алфавіт (33 літери) описано в CHECKLIST крок 4 — використовуй його як
         * окреме коло.
         */
        @ParameterizedTest(name = "[{0}] ШИФР: {1} + {2} = {3}")
        @MethodSource("singleLetterEncryptCases")
        @DisplayName("[ШИФРУВАННЯ] Окремі літери (EN + UA)")
        void encrypt(String lang, String input, int key, String expected) throws IOException {
            assumeLanguageEnabled(lang);
            Path file = createTestFile("letter_" + lang + ".txt", input);

            Path encrypted = execute(ENCRYPT_COMMAND, file, key);

            assertEquals(expected, readFile(encrypted));
        }

        /**
         * <b>Що перевіряє:</b> зворотній зсув по відповідному алфавіту:
         * B-1=A, z-25=a для англ.; Б-1=А, я-32=а для укр.
         *
         * <p><b>Як пройти:</b> реалізуй гілку {@code -d}. Розшифрування з ключем {@code k}
         * — це шифрування з ключем {@code -k}. Алфавіт визначає, як обробляти літери.
         */
        @ParameterizedTest(name = "[{0}] РОЗШИФР: {1} - {2} = {3}")
        @MethodSource("singleLetterDecryptCases")
        @DisplayName("[РОЗШИФРУВАННЯ] Окремі літери (EN + UA)")
        void decrypt(String lang, String input, int key, String expected) throws IOException {
            assumeLanguageEnabled(lang);
            Path file = createTestFile("letter_" + lang + ".txt", input);

            Path decrypted = execute(DECRYPT_COMMAND, file, key);

            assertEquals(expected, readFile(decrypted));
        }

        /**
         * <b>Що перевіряє:</b> повний цикл шифрування → розшифрування над великим
         * текстом повертає оригінал. Використовуються «Гамлет» (англ.) і уривок
         * «1984» Орвелла (укр.).
         *
         * <p><b>Як пройти:</b> базове шифрування і розшифрування мають бути симетричними
         * для будь-якого тексту і ключа: {@code decrypt(encrypt(text, k), k) == text}.
         */
        @ParameterizedTest(name = "[{0}] Цикл encrypt → decrypt повертає оригінал")
        @MethodSource("fixtures")
        @DisplayName("[РОЗШИФРУВАННЯ] Цикл encrypt → decrypt повертає оригінал (EN + UA)")
        void fullCycle(String lang, String fixture) throws IOException {
            assumeLanguageEnabled(lang);
            Path input = createTestFile("fixture_" + lang + ".txt", fixture);

            Path encrypted = execute(ENCRYPT_COMMAND, input, 5);
            Path decrypted = execute(DECRYPT_COMMAND, encrypted, 5);

            assertEquals(fixture, readFile(decrypted),
                    "Цикл encrypt → decrypt не повернув оригінал.");
        }

        /**
         * <b>Що перевіряє:</b> brute-force знаходить правильний ключ і відновлює
         * оригінал ТОЧНО (з урахуванням регістру) для обох мов.
         *
         * <p><b>Як пройти:</b> словниковий підхід (часті слова мови) або частотний
         * аналіз літер. Алгоритм має враховувати мову — для української використовуй
         * частоти або словник української, не англійської.
         */
        @ParameterizedTest(name = "[{0}] Brute-force відновлює оригінал")
        @MethodSource("fixtures")
        @DisplayName("[ПЕРЕБІР КЛЮЧІВ] Знаходить ключ і повертає оригінал (EN + UA)")
        void bruteForce(String lang, String fixture) throws IOException {
            assumeLanguageEnabled(lang);
            Path input = createTestFile("fixture_" + lang + ".txt", fixture);
            Path encrypted = execute(ENCRYPT_COMMAND, input, 5);

            Path bruteForced = execute(BF_COMMAND, encrypted);

            assertEquals(fixture, readFile(bruteForced),
                    "Brute-force не відновив оригінал точно (з урахуванням регістру).");
        }
    }

    @Nested
    @DisplayName("Шифрування: межові випадки")
    class EncryptEdgeCases {

        /**
         * <b>Що перевіряє:</b> шифрування порожнього файлу повертає порожній файл.
         */
        @Test
        @DisplayName("Порожній файл → порожній зашифрований файл")
        void emptyFile() throws IOException {
            Path testFile = createTestFile("empty.txt", "");
            Path encryptedFile = execute(ENCRYPT_COMMAND, testFile, 5);
            assertEquals("", readFile(encryptedFile));
        }

        /**
         * <b>Що перевіряє:</b> цифри (0–9) не входять до алфавіту, тому шифр їх не змінює.
         *
         * <p><b>Як пройти:</b> символи поза алфавітом (цифри, пробіли, пунктуація)
         * мають проходити крізь шифр без змін.
         */
        @Test
        @DisplayName("Файл лише з цифрами не змінюється шифруванням")
        void digitsOnly() throws IOException {
            Path testFile = createTestFile("digits.txt", "0123456789");
            Path encryptedFile = execute(ENCRYPT_COMMAND, testFile, 5);
            assertEquals("0123456789", readFile(encryptedFile));
        }

        /**
         * <b>Що перевіряє:</b> ключ 0 — це нульовий зсув, текст не змінюється.
         */
        @Test
        @DisplayName("Ключ 0 — текст не змінюється")
        void keyZero() throws IOException {
            Path testFile = createTestFile("k0.txt", "Hello, World!");
            Path encryptedFile = execute(ENCRYPT_COMMAND, testFile, 0);
            assertEquals("Hello, World!", readFile(encryptedFile));
        }

        /**
         * <b>Що перевіряє:</b> ключ 26 = повний оберт по 26-літерному алфавіту,
         * тому результат тотожний оригіналу.
         */
        @Test
        @DisplayName("Ключ 26 (повне коло алфавіту) — текст не змінюється")
        void keyFullCycle() throws IOException {
            Path testFile = createTestFile("k26.txt", "Hello");
            Path encryptedFile = execute(ENCRYPT_COMMAND, testFile, 26);
            assertEquals("Hello", readFile(encryptedFile));
        }

        /**
         * <b>Що перевіряє:</b> ключі більші за розмір алфавіту нормалізуються по колу:
         * 27 mod 26 = 1, тому ключ 27 дає той самий результат, що ключ 1.
         */
        @Test
        @DisplayName("Ключ 27 дає той самий результат, що ключ 1")
        void keyOverCycle() throws IOException {
            Path withK1 = execute(ENCRYPT_COMMAND, createTestFile("k1.txt", "Hello"), 1);
            Path withK27 = execute(ENCRYPT_COMMAND, createTestFile("k27.txt", "Hello"), 27);
            assertEquals(readFile(withK1), readFile(withK27));
        }

        /**
         * <b>Що перевіряє:</b> від'ємний ключ -26 = повний оберт у зворотному напрямку,
         * текст не змінюється.
         */
        @Test
        @DisplayName("Ключ -26 (повне коло назад) — текст не змінюється")
        void keyNegativeFullCycle() throws IOException {
            Path testFile = createTestFile("kneg26.txt", "Hello");
            Path encryptedFile = execute(ENCRYPT_COMMAND, testFile, -26);
            assertEquals("Hello", readFile(encryptedFile));
        }

        /**
         * <b>Що перевіряє:</b> від'ємні ключі коректно зсувають по колу в межах
         * власного регістру: A-1=Z, a-1=z, Z-25=A, z-25=a. Тобто великі й малі літери —
         * це два незалежні 26-літерні кола, і при «недольоті» нижче 'A' зсув
         * продовжується з 'Z' того ж регістру (велика залишається великою, мала — малою).
         *
         * <p><b>Як пройти:</b> алгоритм має підтримувати від'ємні ключі. Найпростіше —
         * нормалізувати ключ по модулю розміру алфавіту (26) перед зсувом.
         */
        @DisplayName("Від'ємний ключ зсуває по колу в межах регістру")
        @ParameterizedTest
        @CsvSource({"A, -1, Z", "a, -1, z", "Z, -25, A", "z, -25, a"})
        void negativeKeyWrapsWithinCase(String input, int key, String expected) throws IOException {
            Path testFile = createTestFile("neg.txt", input);

            Path encryptedFile = execute(ENCRYPT_COMMAND, testFile, key);

            assertEquals(expected, readFile(encryptedFile));
        }

        /**
         * <b>Що перевіряє:</b> розділові знаки і пробіли ({@code .,!? \t}) не входять до
         * алфавіту і проходять без змін.
         */
        @Test
        @DisplayName("Розділові знаки і пробіли проходять без змін")
        void specialCharsPassThrough() throws IOException {
            Path testFile = createTestFile("special.txt", ".,!? \t");
            Path encryptedFile = execute(ENCRYPT_COMMAND, testFile, 5);
            assertEquals(".,!? \t", readFile(encryptedFile));
        }

        /**
         * <b>Що перевіряє:</b> перенесення рядків ({@code \n}) зберігаються, а літери
         * на різних рядках шифруються незалежно: «abc\ndef\n» з ключем 1 → «bcd\nefg\n».
         */
        @Test
        @DisplayName("Багаторядковий вміст зберігає переходи рядків")
        void multilineContent() throws IOException {
            Path testFile = createTestFile("multi.txt", "abc\ndef\n");
            Path encryptedFile = execute(ENCRYPT_COMMAND, testFile, 1);
            assertEquals("bcd\nefg\n", readFile(encryptedFile));
        }
    }

    @Nested
    @DisplayName("Збереження початкового файлу")
    class OriginalFileSafety {

        /**
         * <b>Що перевіряє:</b> після виклику {@code -e} оригінальний вхідний файл на диску
         * залишається таким, як був — програма не перезаписує його, а створює окремий файл.
         *
         * <p><b>Як пройти:</b> пиши результат у новий шлях, а вхідний файл лише читай.
         */
        @Test
        @DisplayName("Шифрування не змінює вхідний файл")
        void encryptDoesNotModifyInput() throws IOException {
            String original = "Hello, World!";
            Path testFile = createTestFile("safety.txt", original);
            execute(ENCRYPT_COMMAND, testFile, 5);
            assertEquals(original, Files.readString(testFile));
        }
    }

    @Nested
    @DisplayName("Валідація вхідних даних → CHECKLIST крок 4")
    class ValidationTests {

        /**
         * <b>Що перевіряє:</b> якщо вказаний у {@code -f} файл не існує, програма не
         * падає з винятком і не створює зайвих файлів у поточній директорії.
         *
         * <p><b>Як пройти:</b> {@code Main.main} має ловити будь-який виняток і не
         * пропускати його назовні; на помилку читання файлу не записувати результат.
         */
        @Test
        @DisplayName("Програма не падає і не пише нічого, якщо вхідний файл не існує")
        void fileNotExists() {
            Path fakeFilePath = tempDir.resolve("does-not-exist.txt");
            String[] params = {ENCRYPT_COMMAND, "-f", fakeFilePath.toString(), "-k", "5"};
            List<Path> before = listFiles(tempDir);

            assertDoesNotThrow(() -> Main.main(params),
                    "Очікувалось, що Main коректно обробить неіснуючий файл.");
            assertEquals(before, listFiles(tempDir),
                    "Main не повинен створювати файлів, якщо вхідний файл не існує.");
        }

        /**
         * <b>Що перевіряє:</b> п'ять сценаріїв з невалідними аргументами командного рядка —
         * пропущений {@code -k}, пропущений {@code -f}, пропущена команда, невідомий
         * прапорець, нечислове значення ключа. У всіх випадках програма має:
         * (1) не кидати виняток назовні і (2) не створювати жодного нового файлу.
         *
         * <p><b>Як пройти:</b> Main має ловити виняток парсера і не записувати результат.
         */
        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', textBlock = """
                missing -k       | -e -f {path}
                missing -f       | -e -k 5
                missing command  | -k 5 -f {path}
                unknown flag     | -e -x -k 5 -f {path}
                non-numeric key  | -e -k abc -f {path}
                """)
        @DisplayName("Невалідні аргументи: програма не падає і не створює зайвих файлів")
        void invalidArgsHandled(String scenario, String argsSpec) {
            String[] args = argsSpec.trim()
                    .replace("{path}", inputFilePathEN.toString())
                    .split("\\s+");
            List<Path> before = listFiles(tempDir);
            assertDoesNotThrow(() -> Main.main(args), scenario);
            assertEquals(before, listFiles(tempDir), scenario);
        }
    }
}
