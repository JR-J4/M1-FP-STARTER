package ua.com.javarush.j4;

/** Ukrainian alphabet */
public class UkrainianLanguage implements Language {

    private static final char[] ALPHABET = {
            // uppercase
            'А','Б','В','Г','Ґ','Д','Е','Є','Ж','З','И','І','Ї','Й',
            'К','Л','М','Н','О','П','Р','С','Т','У','Ф','Х','Ц','Ч',
            'Ш','Щ','Ь','Ю','Я',
            // lowercase
            'а','б','в','г','ґ','д','е','є','ж','з','и','і','ї','й',
            'к','л','м','н','о','п','р','с','т','у','ф','х','ц','ч',
            'ш','щ','ь','ю','я'
    };

    private static final String[] COMMON_WORDS = {
            "що", "не", "та", "він", "вона", "але", "як", "це",
            "від", "у", "в", "на", "до", "і", "тому", "була",
            "його", "її", "ми", "ти", "за", "для", "все"
    };

    @Override
    public char[] alphabet() {
        return ALPHABET;
    }

    @Override
    public String[] commonWords() {
        return COMMON_WORDS;
    }
}
