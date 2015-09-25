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

public class ResourceFactory {
    
    private static final RssSuckerLogger logger = 
            new RssSuckerLogger(ResourceFactory.class.getName());    
    
    private static Map<String, Set<String>> wordlists = new TreeMap<String, Set<String>>();
    private static Set<String> missingWordlist = new TreeSet<>();
    
    /** 
     * Load list of lowercased words consisting of only ascii alphabetic chars, 
     * ie chars that can occur in URLs.     
     * Load list form file once, if exists, and return a copy per call.
     * Return null if loading form file fails.
     * @param langCode 
     */
    public static Set<String> getAsciiWordlist(String langCode) {
        if (missingWordlist.contains(langCode)) return null;
        if (wordlists.containsKey(langCode) == false) { 
            boolean result = loadWordlist(langCode);
            if (result == false) {
                missingWordlist.add(langCode);
                return null;
            }
        }        
        Set<String> wlist = wordlists.get(langCode);            
        return new TreeSet<String>(wlist);
    }

    /**
     * Load word list from resources folder, if exists. 
     * File name corresponds to language code. File should be UTF8 encoded, 
     * with one word per line. 
     * @param languageCode ISO 639-1 language code
     */
    private static boolean loadWordlist(String languageCode) {
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
            return true;
        } catch (Exception ex) {
            logger.logErr("Error loading the word list for language: "+languageCode, ex);
            return false;
        } finally {
            try { in.close(); } catch (Exception ex) { }
        }                        
    }
    
}
