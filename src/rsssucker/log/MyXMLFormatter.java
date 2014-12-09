package rsssucker.log;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class MyXMLFormatter extends Formatter {

    private StringBuilder xml;
    
    @Override
    public String getHead(Handler h) {
        return "<log>\n\n";
    }
    
    @Override
    public String getTail(Handler h) {
        return "</log>\n";
    }
    
    @Override
    public String format(LogRecord lr) {
        initXml();
        startTag("record", true);
            
            startTag("date", false);
            print(formatDate(new Date(lr.getMillis())), false);
            endTag("date");
            
            startTag("milis", false);
            print(Long.toString(lr.getMillis()), false);
            endTag("milis");
            
            startTag("sequence", false);
            print(Long.toString(lr.getSequenceNumber()), false);
            endTag("sequence");
            
            startTag("logger", false);
            print(lr.getLoggerName(), false);
            endTag("logger");       
            
            startTag("level", false);
            print(lr.getLevel().toString(), false);
            endTag("level");       
            
            startTag("class", false);
            print(lr.getSourceClassName(), false);
            endTag("class");        
            
            startTag("method", false);
            print(lr.getSourceMethodName(), false);
            endTag("method");                
            
            startTag("thread", false);
            print(Integer.toString(lr.getThreadID()), false);
            endTag("thread");      
            
            startTag("message", true);
            print(lr.getMessage(), true);
            endTag("message");     
        
            Throwable e = lr.getThrown();
            startTag("exception", true);
                startTag("message", true);
                print(e != null ? e.getMessage() : "null", true);
                endTag("message");
                startTag("stacktrace", true);
                print(stackTraceToString(e), false);
                endTag("stacktrace");
            endTag("exception");
        
        endTag("record"); print("", true);
        return getXml();
    }        
    
    private void initXml() { xml = new StringBuilder(); }
    private String getXml() { return xml.toString(); }  
    
    private void startTag(String tag, boolean newline) { 
        xml.append("<"+tag+">");
        if (newline) xml.append("\n"); 
    }
    private void endTag(String tag) { xml.append("</"+tag+">").append("\n"); }
    
    private void print(String s, boolean newline) { 
        xml.append(s); 
        if (newline) xml.append("\n"); 
    }
    
    private String formatDate(Date d) {
        return String.format("%1$td-%1$tm-%1$tY_%1$tH:%1$tM:%1$tS.%1$tL", d);        
    }

    private String stackTraceToString(Throwable t) {
        if (t == null) return "null";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw); 
        t.printStackTrace(pw); pw.flush();
        return sw.getBuffer().toString();
    }
    
}
