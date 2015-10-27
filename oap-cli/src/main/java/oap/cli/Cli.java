/**************************************************************************
 * Copyright (c) Dev Zero G Ltd
 * <p>
 * This software is the confidential and proprietary information of
 * Dev Zero G Ltd ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Dev Zero G Ltd.
 * <p>
 * ***********************************************************************
 */
/*$Id: Options.java,v 1.1 2005/05/10 09:57:29 xpteam-krypton Exp $*/
package oap.cli;


import oap.util.Pair;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class Cli {
    private List<Group> groups = new ArrayList<>();

    public static Cli create() {
        return new Cli();
    }

    public Cli group(String name, Consumer<Map<String, Object>> action, Option<?>... options) {
        Group group = new Group(name, action, options);
        groups.add(group);
        return this;
    }

    public void act(String[] args) {
        act(String.join(" ", args));
    }

    public void act(String args) {
        try {
            List<Pair<String, String>> parameters = new CliParser(new CommonTokenStream(new CliLexer(new ANTLRInputStream(args)))).parameters().list;
            Optional<Group> group = groups.stream().filter(g -> g.matches(parameters)).findFirst();
            if (group.isPresent()) group.get().act(parameters).ifFailure(failure -> {
                System.out.println("Error: " + failure);
                printHelp();
            });
            else printHelp();
        } catch (RecognitionException e) {
            System.err.println("Error: " + e.getMessage());
            printHelp();
        }
    }

    private void printHelp() {
        for (Group group : groups) {
            System.out.println(group.name);
            for (Option<?> option : group.options()) {
                System.out.println("\t\t" + option);
            }
        }
    }
}
