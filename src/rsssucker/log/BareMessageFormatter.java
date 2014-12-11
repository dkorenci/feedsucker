package rsssucker.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/** Output only LogRecord's message in a single line. */
public class BareMessageFormatter extends Formatter {

    @Override
    public String format(LogRecord lr) {
        String m = lr.getMessage();
        if (!m.endsWith("\n")) m = m+"\n";
        return m;
    }
    
}
