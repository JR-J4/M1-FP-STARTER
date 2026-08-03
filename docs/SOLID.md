# SOLID у проєкті «Криптоаналізатор»

Мапа принципів SOLID до конкретного коду. У самих класах ці місця позначені
коментарями з міткою `// ── SOLID ▸ ...`. Знайти їх усі можна командою:

```bash
grep -rn "SOLID" src/main/java
```

**Головний висновок:** усі п'ять принципів уже присутні в проєкті в сильній формі
(шифри, скорери та читачі — це готові приклади «Стратегії», команди — «Шаблонний
метод», `CryptoService` — «корінь композиції»). Тому окремі демо-класи не додавалися;
натомість кожен принцип позначено коментарем у канонічному місці. Додатково лишили
**живий приклад порушення ISP** у `TextReader` — як контраст «правильно/неправильно».

---

## S — Single Responsibility Principle (Принцип єдиного обов'язку)

Клас має мати лише одну причину для зміни.

| Клас | Єдиний обов'язок |
|------|------------------|
| `alphabet/CharacterRing` | арифметика одного кільця символів (зсув, дзеркало, індекс) |
| `io/OutputNaming` | обчислення імені вихідного файлу ([ENCRYPTED]/[DECRYPTED]) |
| `io/TextWriter` | запис тексту у файл |
| `io/TextReader` (+реалізації) | читання тексту з файлу |
| `crack/LanguageDetector` | визначення мови тексту |
| `crack/CrackResult`, `app/CryptoRequest` | незмінні носії даних (record) |

Читання, іменування та запис навмисно розділені на **три окремі класи** — класична
демонстрація SRP.

## O — Open/Closed Principle (Принцип відкритості/закритості)

Відкрито для розширення, закрито для модифікації.

| Точка розширення | Як розширюють без зміни наявного коду |
|------------------|----------------------------------------|
| `cipher/Cipher` | новий шифр = новий клас (`CaesarCipher`, `Rot13Cipher`, `AtbashCipher`, `VigenereCipher`) |
| `crack/FitnessScorer` | нова метрика = новий клас (`DictionaryScorer`, `FrequencyScorer`) |
| `io/TextReader` | новий формат = новий читач (`PlainTextReader`, `MarkdownReader`, `GzipTextReader`) |

Компроміс: `cipher/CipherFactory` та `Alphabets.byName` містять `switch`. Це свідома
**ізоляція** єдиного місця, яке треба доповнити (один `case`), — решта застосунку
лишається незмінною.

## L — Liskov Substitution Principle (Принцип підстановки Лісков)

Підклас можна підставити замість базового типу без зміни коректності.

| Базовий тип | Взаємозамінні реалізації |
|-------------|--------------------------|
| `app/command/CryptoCommand` | `EncryptCommand`, `DecryptCommand`, `BruteForceCommand` — `CryptoService` кличе `execute()`, не питаючи, який це підклас |
| `cipher/Cipher` | усі шифри чесно тримають контракт `String → String`; `Rot13Cipher` реалізовано **делегуванням** до Caesar (композиція замість наслідування) |

## I — Interface Segregation Principle (Принцип розділення інтерфейсів)

Багато тонких інтерфейсів кращі за один «товстий».

**Правильно (тонкі інтерфейси):** `cipher/Cipher` (encrypt/decrypt),
`crack/FitnessScorer` (score), `crack/Cracker` (crack).

**Порушення — навчальний контрприклад:** `io/TextReader extends Comparable<TextReader>`
з методом `getSupportedFileTypes()`. Клієнт `TextReaders.pick()` користується лише
`supports()`+`read()`, але кожна реалізація змушена писати ще й `compareTo()` та
`getSupportedFileTypes()`, яких ніхто не викликає. Як виправити — винести потребу
сортування/переліку типів у окремий вузький інтерфейс і додавати його лише тим
реалізаціям, яким він справді потрібен (детальний коментар — у `TextReader.java`).

## D — Dependency Inversion Principle (Принцип інверсії залежностей)

Залежати від абстракцій, а не від конкретики. Деталі впроваджуються ззовні.

| Хто | Від чого залежить (абстракція) | Як впроваджується |
|-----|-------------------------------|-------------------|
| `crack/CaesarCracker` | `FitnessScorer` | через конструктор (Constructor Injection) |
| `app/command/EncryptCommand`/`DecryptCommand` | `Cipher` | через конструктор |
| `app/command/CryptoCommand` | `TextReader` (через `TextReaders`) | через конструктор |
| `app/CryptoService` | — | **корінь композиції**: створює конкретні реалізації й зшиває їх із командами |

---

### Патерни проєктування, що підпирають SOLID

- **Стратегія** — `Cipher`, `FitnessScorer`, `TextReader` (OCP + DIP).
- **Шаблонний метод** — `CryptoCommand.execute()` фіксує кроки read→transform→write (LSP).
- **Фабрика** — `CipherFactory` ізолює створення (OCP).
- **Фасад / корінь композиції** — `CryptoService` (DIP).
- **Реєстр** — `Alphabets`, `LanguageProfiles`, `TextReaders`.
