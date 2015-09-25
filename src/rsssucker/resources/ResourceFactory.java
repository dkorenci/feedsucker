/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rsssucker.core.RssSuckerApp;
import rsssucker.log.RssSuckerLogger;

/**
 *
 * @author damir
 */
public class ResourceFactory {
    
    private static final RssSuckerLogger logger = 
            new RssSuckerLogger(ResourceFactory.class.getName());    
    
    private static Map<String, Set<String>> wordlists = new TreeMap<String, Set<String>>();
    
    /** 
     * Load list of lowercased words consisting of only ascii alphabetic chars, 
     * ie chars that can occur in URLs.     
     * Load list form file once, if exists, and return a copy per call.
     * Return null if loading form file fails.
     */
    public static Set<String> getCroatianAsciiWordlist() {
        if (wordlists.containsKey("hr") == false) loadWordlist("hr");
        if (wordlists.containsKey("hr")) {
            Set<String> wlist = wordlists.get("hr");            
            return new TreeSet<String>(wlist);
        }
        else return null;        
    }

    // 
    /**
     * Load word list from resources folder, if exists. 
     * File name corresponds to language code. File should be UTF8 encoded, 
     * with one word per line. 
     * @param languageCode ISO 639-1 language code
     */
    private static void loadWordlist(String languageCode) {
        BufferedReader in = null;
        try {
            File f = new File("resources/"+languageCode+"_ascii_wordlist.txt");
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF8"));
            String line;
            Set<String> wordlist = new TreeSet<String>();
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if ("".equals(line)) continue;
                wordlist.add(line);
            }
            wordlists.put(languageCode, wordlist);
        } catch (Exception ex) {
            logger.logErr("Error loading the word list for language: "+languageCode, ex);
        } finally {
            try { in.close(); } catch (Exception ex) { }
        }                        
    }
    
}
