package ua.com.javarush.j4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class CaesarCipher {

    private static final String ENG_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String UA_ALPHABET  = "абвгґдеєжзиіїйклмнопрстуфхцчшщьюяАБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯ";


    public char encryptChar(char ch, int key) {
        String strongAlphabet = null;

        // Определяем, к какому алфавиту относится символ
        if(ENG_ALPHABET.indexOf(ch) != -1) strongAlphabet = ENG_ALPHABET;
          else if(UA_ALPHABET.indexOf(ch) != -1) strongAlphabet = UA_ALPHABET;

        if(strongAlphabet == null) {
            return ch;
        }

        int alphabetLength = strongAlphabet.length();
        int index = strongAlphabet.indexOf(ch);
        // В Java оператор % возвращает остаток от деления, который для отрицательных чисел тоже будет отрицательным.
// Например: -5 % 26 в Java вернет -5. А отрицательного индекса в строке алфавита не существует (будет ошибка!).
// ДАННАЯ ФОРМУЛА  — это метод, чтобы превратить любой сдвиг в безопасное число от 0 до (ДЛИНА_АЛФАВИТА - 1):
// 1. (key % alphabetLength) удерживает сдвиг в пределах одного круга алфавита (от -длина до +длина).
// 2. Затем прибавляем   внутри скобок alphabetLength, чтобы гарантированно перевести отрицательный результат в плюс.
// 3. Внешний % alphabetLength нужен на случай, если исходный ключ изначально был положительным — он сбрасывает лишний круг.
// Внешний % alphabetLength для отрицательного ключа тоже нужен: если при сложении получилась ровно длина алфавита (например, key = -26, а длина алфавита 26),
// Тогда  1)  - key % alphabetLength ---> -26 % 26 дает 0.
//        2)  Прибавляем отрицательное смещение к длине алфавита ---->  0 + 26 дает 26.
//        3)  Получили safetyKey = 26.Но в алфавите длиной 26 индексы идут строго от 0 до 25. Символа под номером 26 нет.
//  Программа упадет с ошибкой.
// Внешний процент берет эту получившуюся 26 и делает: 26 % 26 ---> получаем 0.
// Получили валидный нулевой индекс.
        int safetyKey = (key % alphabetLength + alphabetLength) % alphabetLength;

        // Вычисляем новый индекс символа по кругу
        int newIndex = (index + safetyKey) % alphabetLength;

        char resultChar = strongAlphabet.charAt(newIndex);
        // Возвращаем полученный зашифрованный/расшифрованный символ
        return  resultChar;

    }
    /**
     *  Идём  по всей строке и собираем новую строку из измененных символов.
     */
    public String encryptString(String text, int key) {

        StringBuilder result = new StringBuilder();

        for(int i=0; i < text.length();i++){
            char originalChar = text.charAt(i);
            char encryptedChar = encryptChar(originalChar, key);

            result.append(encryptedChar); // Посимвольно собираем результат в буфер
        }
     return result.toString();

    }
    /**
     * Главный , по сути ,метод: читает файл, рассчитывает ключ, меняет имя и записывает результат.
     */

     public void execute(RunOptions options) {
        try {
            // 1. Получаем путь к файлу из переданных аргументов и читаем его содержимое в строку
            Path sourcePath = options.getFilePath();
            String content = Files.readString(sourcePath);
            int newKey;
            // 2. Рассчитываем итоговый шаг сдвига (newKey) в зависимости от выбранной команды
               if(options.getCommand() == Command.BRUTE_FORCE) {
                   // Если брутфорс — передаем текст и текущий объект шифратора (this) для подбора ключа
                   int foundKey = BruteForce.findKey(content, this);
                   newKey = -foundKey;  // foundKey - оптимально подобранный ключ.Так как мы расшифровываем, сдвиг идет в минус
               } else if(options.getCommand() == Command.DECRYPT) {
                   // Если обычное дешифрование — инвертируем ключ пользователя в минус
                   newKey = -options.getKey();
               } else {
                   // Если шифрование — используем ключ пользователя как есть (в плюс)
                   newKey = options.getKey();
               }
            // 3. Запускаем обработку всего текста с вычисленным ключом
            String encryptedContent = encryptString(content, newKey);
            // 4. Получаем чистое имя первичного файла
            String originalName =  sourcePath.getFileName().toString();
            // Чистим старые теги из имени файла
            String cleanName = originalName.replace("[ENCRYPTED]", "").replace("[DECRYPTED]", "");
            // Определяем, какой суффикс приклеить к новому файлу
            String correct = (options.getCommand() == Command.DECRYPT || options.getCommand() == Command.BRUTE_FORCE) ? "[DECRYPTED]" : "[ENCRYPTED]";
            String newName ;

            int dotIndex = cleanName.lastIndexOf('.'); // Ищем, где начинается расширение (например, .txt)
            if(dotIndex != -1) {
                // Если расширение есть — вставляем тег ПЕРЕД точкой (было: text.txt -> стало: text[DECRYPTED].txt)
                newName = cleanName.substring(0, dotIndex) + correct + cleanName.substring(dotIndex);

               } else {
                // Если расширения нет — просто ставим  тег в самый конец
                newName = cleanName + correct;
            }
            // 5. Метод resolveSibling отрезает старое имя файла из пути и вставляет туда новое.
            // Благодаря этому новый файл сохранится ровно в ту же папку, где лежал оригинал.
            Path outPath = sourcePath.resolveSibling(newName);
            Files.writeString(outPath, encryptedContent);

           } catch (IOException e){
            System.out.println("Ошибка при работе с файлом :" +  e.getMessage());
        }
     }
}


