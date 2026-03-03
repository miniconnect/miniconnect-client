package hu.webarticum.miniconnect.client.repl;

import org.jline.reader.impl.DefaultParser;

public class SafeJlineParser extends DefaultParser {

    @Override
    public boolean isEscapeChar(char ch) {
        return false;
    }

}
