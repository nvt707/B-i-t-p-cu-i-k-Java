package com.caro.common;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.File;

public class ConfigReader {
    public static String get(String tag) {
        try {
            File xmlFile = new File("config.xml"); // IOStream
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            return doc.getElementsByTagName(tag).item(0).getTextContent();
        } catch (Exception e) {
            return "8888"; // Default fallback
        }
    }
}