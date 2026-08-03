package ua.com.javarush.j4;

import ua.com.javarush.j4.app.command.Animal;
import ua.com.javarush.j4.app.command.CryptoCommand;
import ua.com.javarush.j4.cli.CryptoCli;
import ua.com.javarush.j4.io.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

/**
 * Entry point. Delegates to the picocli front end, which parses arguments and
 * runs the requested command. Never propagates exceptions for bad input.
 */
public class Main {
    public static void main(String[] args) throws CloneNotSupportedException {

       MarkdownReader mdReader = new MarkdownReader();


        Class<? extends MarkdownReader> aClass = mdReader.getClass();


        String name = aClass.getName();

        System.out.println(name);

        System.out.println("");

        int a = 2;


//        new CryptoCli().run(args);


//        List<String> hello = List.of("Hello", "World");

//        MarkdownReader mdReader = new MarkdownReader();
//        MarkdownReader mdReader2 = new MarkdownReader();
//
//        System.out.println(mdReader.equals(mdReader2));

//        hello.forEach( s -> System.out.println(s) );
//
//        hello.forEach( System.out::println );

    }


}
