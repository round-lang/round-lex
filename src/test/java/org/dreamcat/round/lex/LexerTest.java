package org.dreamcat.round.lex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.dreamcat.common.io.ClassPathUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * gradle cleanTest && gradle test --tests org.dreamcat.round.lex.LexerTest.test
 *
 * @author Jerry Will
 * @version 2021-08-15
 */
class LexerTest {

    private String expression;

    @BeforeEach
    void init() throws IOException {
        File projectDir = new File(".").getCanonicalFile();
        assert projectDir.getName().equals("round-lex");

        String pkg = getClass().getPackage().getName().replace('.', '/');
        String clsJava = getClass().getSimpleName() + ".java";
        File currentFile = new File(projectDir, "src/test/java/" + pkg + "/" + clsJava);
        assert currentFile.exists();

        File resourceFile = new File(projectDir, "build/classes/java/test/" + clsJava);
        Files.copy(currentFile.toPath(), resourceFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        this.expression = ClassPathUtil.getResourceAsString(clsJava);
    }

    @Test
    void test() {
        Lexer lexer = new Lexer(new LexConfig().setEnableTokenInfo(true));
        TokenStream stream = lexer.lex(expression);
        while (stream.hasNext()) {
            System.out.println(stream.get());
            stream.next();
        }
    }

    @Test
    void testLazily() {
        Lexer lexer = new Lexer(new LexConfig().setEnableLazy(true).setEnableTokenInfo(true));
        TokenStream stream = lexer.lex(expression);
        while (stream.hasNext()) {
            try {
                stream.throwWrongSyntax();
            } catch (Exception e) {
                // System.out.println(e.getClass() + ": " + e.getMessage());
            }
            System.out.println(stream.get());
            stream.next();
        }
    }
}