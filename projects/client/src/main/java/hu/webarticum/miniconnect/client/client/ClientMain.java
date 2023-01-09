package hu.webarticum.miniconnect.client.client;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.jline.reader.Highlighter;

import hu.webarticum.miniconnect.api.MiniSession;
import hu.webarticum.miniconnect.api.MiniSessionManager;
import hu.webarticum.miniconnect.client.repl.AnsiUtil;
import hu.webarticum.miniconnect.client.repl.HostPortInputRepl;
import hu.webarticum.miniconnect.client.repl.KeywordCompleter;
import hu.webarticum.miniconnect.client.repl.PatternHighlighter;
import hu.webarticum.miniconnect.client.repl.PlainReplRunner;
import hu.webarticum.miniconnect.client.repl.Repl;
import hu.webarticum.miniconnect.client.repl.ReplRunner;
import hu.webarticum.miniconnect.client.repl.RichReplRunner;
import hu.webarticum.miniconnect.lang.ImmutableMap;
import hu.webarticum.miniconnect.messenger.adapter.MessengerSessionManager;
import hu.webarticum.miniconnect.server.ClientMessenger;
import hu.webarticum.miniconnect.server.ServerConstants;
import hu.webarticum.regexbee.Bee;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "mini-repl")
public class ClientMain implements Callable<Integer> {

    private static final String DEFAULT_HOST = "localhost";

    private static final int DEFAULT_PORT = ServerConstants.DEFAULT_PORT;

   
    @Parameters(
            index = "0",
            description = "Server address",
            defaultValue = "")
    public String serverAddressArg;

    @Option(
            names = { "-i", "--interactive-input" },
            arity = "0..1",
            description = "Get server host and port interactively",
            defaultValue = "false")
    public boolean interactiveInputArg;
    

    public static void main(String[] args) {
        int exitCode = new CommandLine(ClientMain.class).execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        boolean runHostPortInputRepl = interactiveInputArg;
        if (serverAddressArg != null && !serverAddressArg.isEmpty()) {
            int colonPos = serverAddressArg.indexOf(':');
            if (colonPos != -1) {
                host = serverAddressArg.substring(0, colonPos);
                try {
                    port = Integer.parseInt(serverAddressArg.substring(colonPos + 1));
                } catch (NumberFormatException e) {
                    runHostPortInputRepl = true;
                }
            } else {
                host = serverAddressArg;
            }
        }
        ReplRunner replRunner = createReplRunner();
        if (runHostPortInputRepl) {
            HostPortInputRepl hostPortInputRepl = new HostPortInputRepl(host, port);
            replRunner.run(hostPortInputRepl);
            host = hostPortInputRepl.getHost();
            port = hostPortInputRepl.getPort();
        }
        try (ClientMessenger clientMessenger = new ClientMessenger(host, port)) {
            MiniSessionManager sessionManager = new MessengerSessionManager(clientMessenger);
            try (MiniSession session = sessionManager.openSession()) {
                String titleMessage = SqlRepl.DEFAULT_TITLE_MESSAGE + " - " + host + ":" + port;
                Repl repl = new SqlRepl(session, titleMessage);
                replRunner.run(repl);
            }
        }
        return 0;
    }
    
    private static ReplRunner createReplRunner() {
        if (System.console() != null) {
            return new RichReplRunner(createHighlighter(), new KeywordCompleter(SqlRepl.KEYWORDS));
        }
        
        return new PlainReplRunner(System.in, System.out); // NOSONAR System.out is necessary
    }
    
    private static Highlighter createHighlighter() {
        Pattern pattern =
                Bee
                        .then(Bee.DEFAULT_WORD_BOUNDARY)
                        .then(Bee.oneFixedOf(SqlRepl.KEYWORDS.asList())
                        .then(Bee.DEFAULT_WORD_BOUNDARY)
                        .as("keyword"))
                .or(Bee.fixedChar('@').then(Bee.ASCII_WORD).as("variable"))
                .or(Bee.quoted('\'', '\\').as("singlequoted"))
                .or(Bee.quoted('"', '\\').as("doublequoted"))
                .or(Bee.quoted('`', '`').as("backticked"))
                .toPattern(Pattern.CASE_INSENSITIVE);
        ImmutableMap<String, Function<String, String>> formatters = ImmutableMap.of(
                "keyword", AnsiUtil::formatAsKeyword,
                "variable", AnsiUtil::formatAsVariable,
                "singlequoted", AnsiUtil::formatAsString,
                "doublequoted", AnsiUtil::formatAsQuotedIdentifier,
                "backticked", AnsiUtil::formatAsQuotedIdentifier);
        return new PatternHighlighter(pattern, formatters);
    }

}
