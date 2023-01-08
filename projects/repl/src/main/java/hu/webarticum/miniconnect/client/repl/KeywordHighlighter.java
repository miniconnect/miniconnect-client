package hu.webarticum.miniconnect.client.repl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;

import hu.webarticum.miniconnect.lang.ImmutableList;
import hu.webarticum.regexbee.Bee;

public class KeywordHighlighter implements Highlighter {
    
    private final Pattern keywordPattern;
    
    
    public KeywordHighlighter(ImmutableList<String> keywords) {
        this.keywordPattern = Bee
                .then(Bee.DEFAULT_WORD_BOUNDARY)
                .then(Bee.oneFixedOf(keywords.asList()))
                .then(Bee.DEFAULT_WORD_BOUNDARY)
                .toPattern(Pattern.CASE_INSENSITIVE);
    }
    

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        StringBuffer resultBuffer = new StringBuffer();
        Matcher matcher = keywordPattern.matcher(buffer);
        while (matcher.find()) {
            String word = matcher.group();
            matcher.appendReplacement(resultBuffer, AnsiUtil.formatAsKeyword(word));
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
