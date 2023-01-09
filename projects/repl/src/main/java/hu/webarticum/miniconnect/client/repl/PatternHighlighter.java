package hu.webarticum.miniconnect.client.repl;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;

import hu.webarticum.miniconnect.lang.ImmutableMap;

public class PatternHighlighter implements Highlighter {
    
    private final Pattern pattern;
    
    private final ImmutableMap<String, Function<String, String>> formatters;
    
    
    public PatternHighlighter(Pattern pattern, ImmutableMap<String, Function<String, String>> formatters) {
        this.pattern = pattern;
        this.formatters = formatters;
    }
    

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        StringBuffer resultBuffer = new StringBuffer();
        Matcher matcher = pattern.matcher(buffer);
        while (matcher.find()) {
            for (String groupName : formatters.keySet()) {
                String part = matcher.group(groupName);
                if (part != null) {
                    matcher.appendReplacement(resultBuffer, formatters.get(groupName).apply(part));
                    break;
                }
            }
        }
        matcher.appendTail(resultBuffer);
        return AttributedString.fromAnsi(resultBuffer.toString());
    }

    @Override
    public void setErrorPattern(Pattern errorPattern) {
        // nothing to do
    }

    @Override
    public void setErrorIndex(int errorIndex) {
        // nothing to do
    }

}
