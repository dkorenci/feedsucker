package feedsucker.article.newspaper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import feedsucker.article.ArticleData;
import feedsucker.article.IArticleScraper;
import feedsucker.util.PropertiesReader;
import feedsucker.config.FeedsuckerConfig;

/**
 * Wrapper for news article scraping functionality in python newspaper package.
 */
public class Newspaper implements IArticleScraper {        
    
    private static final String titleEndMarker = "!-TITLE-END-!";
    private static final String endMarker = "!-END-!";
    private static final String terminateCommand = "EXIT";
    private static final String[] errorRegex = {".*\\[.*ERR.*\\].*"};
    
    Process process;    
    OutputStreamWriter procIn;     
    BufferedReader procOut;
    private final String language;
    
    /** Initialize new newspaper instance. */
    public Newspaper(String langCode) throws IOException {
        language = langCode;
        PropertiesReader properties = new PropertiesReader(FeedsuckerConfig.propertiesFile);        
        String interfaceScript = properties.getProperty("newspaper_interface");
        String pythonCommand = properties.getProperty("python_command");        
        process = Runtime.getRuntime().exec(pythonCommand + " " + interfaceScript);      
        procIn = new OutputStreamWriter(process.getOutputStream(), "UTF-8");                  
        procOut = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        // signal article language code, this must be the first line of the input
        procIn.append(language+"\n").flush();          
    }    
    
    @Override
    public synchronized ArticleData scrapeArticle(String url) throws IOException, NewspaperException {
        url = url.trim();          
        boolean errorOccured = false;                
        String line, title = ""; StringBuilder text = new StringBuilder();

        // write url on a single line of script input                  
        procIn.append(url+"\n").flush();        
        // read and process output        
        boolean readTitle = true;        
         //Charset.forName("UTF-8")
        while (true) {    
            line = procOut.readLine();
            if (line == null) { 
                throw new IOException("output stream ended unexpectedly.\n"
                        + "received text: " + text.toString());
            }
            if (isErrorMessage(line)) errorOccured = true;       
            if (line.equals(endMarker)) break;
            //System.out.println(line);                     
            if (line.equals(titleEndMarker)) {
                readTitle = false;
                continue;
            }
            if (readTitle) title = title + line + "\n";
            else {                
                text.append(line).append("\n");
            }
        }
        
        ArticleData result = new ArticleData();
        result.setTitle(title.trim()); result.setText(text.toString().trim());
        
        if (errorOccured) {
            throw new NewspaperException("newspaper output: \n"+
                    result.getTitle()+result.getText());
        }
        
        return result;        
    }
    
    // return true if a string (from newspaper output) produces an error message
    private static boolean isErrorMessage(String str) {
        for (String regex : errorRegex) {
            if (str.matches(regex)) return true;
        }
        return false;
    }
    
    /** Terminate the script, return exit status. */
    public synchronized int close() {        
        try { // send terminate command and wait for process to terminate
            procIn.append(terminateCommand+"\n").flush();
            Thread.sleep(5);
        }
        catch (Exception ex) { process.destroy(); return 1; } // kill process
        // return exit value
        try { 
            int exitVal = process.exitValue();        
            return exitVal;
        }
        catch (IllegalThreadStateException ex) {
            // if process is still running, kill it
            process.destroy(); return 1;
        }
    }
    
}
