package rsssucker.article.newspaper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Wrapper for python newspaper package.
 */
public class Newspaper {
    
    private static final String script = "newspaper/extract.py";
    
    private static final String titleEndMarker = "!-TITLE-END-!";
    private static final String terminateCommand = "EXIT";
    private static final String[] errorRegex = {".*\\[.*ERR.*\\].*"};
    
    Process process;
    BufferedReader procOut;
    OutputStreamWriter procIn;            
    
    public Newspaper() throws IOException {
        process = Runtime.getRuntime().exec("python "+script);
        procOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        procIn = new OutputStreamWriter(process.getOutputStream());        
    }    
    
    public NewspaperOutput processUrl(String url) throws IOException, NewspaperException {
        url = url.trim();
        // write url on a single line of script input
        procIn.append(url+"\n").flush();        
        // read and process output
        String line, title = ""; StringBuilder text = new StringBuilder();
        boolean readTitle = true, errorOccured = false;
        while ((line = procOut.readLine()) != null) {    
            errorOccured = isErrorMessage(line);
            if (line.equals(titleEndMarker)) {
                readTitle = false;
                continue;
            }
            if (readTitle) title = title + line + "\n";
            else {
                text.append(line).append("\n");
            }
        }
        NewspaperOutput result = new NewspaperOutput();
        result.setTitle(title); result.setText(text.toString());
        
        if (errorOccured) {
            throw new NewspaperException("newspaper output: \n"+
                    result.getTitle()+result.getText());
        }
        
        return result;
    }
    
    // return true if a string (from newspaper output) produces an error message
    public static boolean isErrorMessage(String str) {
        for (String regex : errorRegex) {
            if (str.matches(regex)) return true;
        }
        return false;
    }
    
    /** Terminate the script, return exit status. */
    public int close() {        
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
