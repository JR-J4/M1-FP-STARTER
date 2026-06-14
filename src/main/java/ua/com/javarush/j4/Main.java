package ua.com.javarush.j4;

import ua.com.javarush.j4.application.ApplicationService;

public class Main {
    public static void main(String[] args) {

        ApplicationService app = new ApplicationService();
        app.run(args);
    }
}