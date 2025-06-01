package com.example.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Appconfig {
    public static void loadEnv() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(".env"));

            for (String name : props.stringPropertyNames()) {
                String value = props.getProperty(name);
                System.setProperty(name, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
