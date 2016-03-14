package feedsucker.log;

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
        startTag("record"); nline();
            
            startTag("date");
            print(formatDate(new Date(lr.getMillis())), false);
            endTag("date");
            
            startTag("milis");
            print(Long.toString(lr.getMillis()), false);
            endTag("milis");
            
            startTag("sequence");
            print(Long.toString(lr.getSequenceNumber()), false);
            endTag("sequence");
            
            startTag("logger");
            print(lr.getLoggerName(), false);
            endTag("logger");       
            
            startTag("level");
            print(lr.getLevel().toString(), false);
            endTag("level");       
            
            startTag("class");
            print(lr.getSourceClassName(), false);
            endTag("class");        
            
            startTag("method"); startCdata(" "," ");
            print(lr.getSourceMethodName(), false); 
            endCdata(" "," "); endTag("method");                
            
            startTag("thread");
            print(Integer.toString(lr.getThreadID()), false);
            endTag("thread");      
            
            startTag("message"); startCdata(" ",""); nline();
            print(lr.getMessage(), true);
            endCdata(""," "); endTag("message");     
        
            Throwable e = lr.getThrown();
            startTag("exception"); nline();
                startTag("message"); startCdata(" ",""); nline();
                print(e != null ? e.getMessage() : "null", true);
                endCdata(""," "); endTag("message");
                startTag("stacktrace"); startCdata(" ","");  nline();
                print(stackTraceToString(e), false);
                endCdata(""," "); endTag("stacktrace");
            endTag("exception");
        
        endTag("record"); print("", true);
        return getXml();
    }        
    
    private void initXml() { xml = new StringBuilder(); }
    private String getXml() { return xml.toString(); }  
    
    private static String cdata(String s) {
        return "<![CDATA[   "+s+"   ]]>";
    }
    
    private static String blockCdata(String s) {
        return "<![CDATA[\n"+s+"\n]]>";
    }    
    
    private void startTag(String tag) { 
        xml.append("<"+tag+">");      
    }
    private void nline() { xml.append("\n"); }
    private void startCdata(String p, String s) { xml.append(p).append("<![CDATA[").append(s); }
    private void endCdata(String p, String s) { xml.append(p).append("]]>").append(s); }
    
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
