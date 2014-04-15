/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.framework.ui.command;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.esa.beam.framework.ui.command.DefaultCommandMenuInserter.sortCommandsByAnchor;
import static org.esa.beam.framework.ui.command.DefaultCommandMenuInserter.sortCommandsByName;
import static org.esa.beam.framework.ui.command.DefaultCommandMenuInserterTest.Anchor.AFTER;
import static org.esa.beam.framework.ui.command.DefaultCommandMenuInserterTest.Anchor.BEFORE;
import static org.esa.beam.framework.ui.command.DefaultCommandMenuInserterTest.Anchor.FIRST;
import static org.esa.beam.framework.ui.command.DefaultCommandMenuInserterTest.Anchor.LAST;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("ALL")
public class DefaultCommandMenuInserterTest {

    public enum Anchor {
        FIRST,
        LAST,
        BEFORE,
        AFTER
    }

    @Test
    public void testSortByAnchor_FirstOnly_3() {

        List<Command> commands = toList(createCommand("a", FIRST),
                                        createCommand("b", FIRST),
                                        createCommand("c", FIRST));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_LastOnly_3() {

        List<Command> commands = toList(createCommand("a", LAST),
                                        createCommand("b", LAST),
                                        createCommand("c", LAST));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_FirstAndLast_6() {

        List<Command> commands = toList(createCommand("a", FIRST),
                                        createCommand("b", FIRST),
                                        createCommand("c", FIRST),
                                        createCommand("d", LAST),
                                        createCommand("e", LAST),
                                        createCommand("f", LAST));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
        assertEquals("d", commands.get(index++).getCommandID());
        assertEquals("e", commands.get(index++).getCommandID());
        assertEquals("f", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_FirstAndLast_2() {

        List<Command> commands = toList(createCommand("a", FIRST),
                                        createCommand("b", LAST));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_BeforeAndLast_2() {

        List<Command> commands = toList(createCommand("a", BEFORE, "b"),
                                        createCommand("b", LAST, null));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_BeforeAndLast_3() {

        List<Command> commands = toList(createCommand("a", BEFORE, "c"),
                                        createCommand("b", BEFORE, "c"),
                                        createCommand("c", LAST));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_FirstAndAfter_3() {

        List<Command> commands = toList(createCommand("a", FIRST),
                                        createCommand("b", AFTER, "a"),
                                        createCommand("c", AFTER, "a"));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_NoneAndAfter_2() {

        List<Command> commands = toList(createCommand("a"),
                                        createCommand("b", AFTER, "a"));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_NoneAndAfter_3() {

        List<Command> commands = toList(createCommand("a"),
                                        createCommand("b", AFTER, "a"),
                                        createCommand("c", AFTER, "b"));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_NoneAndAfter_5() {

        List<Command> commands = toList(createCommand("a"),
                                        createCommand("b", AFTER, "a"),
                                        createCommand("c", AFTER, "b"),
                                        createCommand("d", AFTER, "c"),
                                        createCommand("e", AFTER, "d"));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
        assertEquals("d", commands.get(index++).getCommandID());
        assertEquals("e", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_AllKinds_4() {

        List<Command> commands = toList(createCommand("a", FIRST),
                                        createCommand("b", AFTER, "a"),
                                        createCommand("c", BEFORE, "d"),
                                        createCommand("d", LAST, null));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
        assertEquals("d", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByAnchor_AllKinds_12() {

        List<Command> commands = toList(createCommand("a", FIRST),
                                        createCommand("b", FIRST),
                                        createCommand("c", FIRST),
                                        createCommand("d", AFTER, "c"),
                                        createCommand("e", AFTER, "c"),
                                        createCommand("f", AFTER, "c"),
                                        createCommand("g", BEFORE, "j"),
                                        createCommand("h", BEFORE, "j"),
                                        createCommand("i", BEFORE, "j"),
                                        createCommand("j", LAST),
                                        createCommand("k", LAST),
                                        createCommand("l", LAST));
        reverseList(commands);
        sortCommandsByAnchor(commands);

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
        assertEquals("d", commands.get(index++).getCommandID());
        assertEquals("e", commands.get(index++).getCommandID());
        assertEquals("f", commands.get(index++).getCommandID());
        assertEquals("g", commands.get(index++).getCommandID());
        assertEquals("h", commands.get(index++).getCommandID());
        assertEquals("i", commands.get(index++).getCommandID());
        assertEquals("j", commands.get(index++).getCommandID());
        assertEquals("k", commands.get(index++).getCommandID());
        assertEquals("l", commands.get(index++).getCommandID());
    }

    @Test
    public void testSortByName() {

        List<Command> commands = toList(createCommand("a"),
                                        createCommand("b"),
                                        createCommand("c"),
                                        createCommand("d"));
        reverseList(commands);

        int index = 0;
        assertEquals("d", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("a", commands.get(index++).getCommandID());

        sortCommandsByName(commands);

        index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
        assertEquals("d", commands.get(index++).getCommandID());
    }

    /////////////////////////////////////////
    // Helpers

    private static void reverseList(List<Command> commands) {
        for (int i1 = 0; i1 < commands.size() / 2; i1++) {
            int i2 = commands.size() - i1 - 1;
            Command command1 = commands.get(i1);
            Command command2 = commands.get(i2);
            commands.set(i1, command2);
            commands.set(i2, command1);
        }
    }

    static List<Command> toList(Command... commands) {
        return new ArrayList<>(Arrays.asList(commands));
    }

    static Command createCommand(String name) {
        return createCommand(name, null, null);
    }

    static Command createCommand(String name, Anchor anchor) {
        return createCommand(name, anchor, null);
    }

    static Command createCommand(String name, Anchor anchor, String relativeId) {
        ExecCommand command = new ExecCommand(name);
        command.setText(name);
        if (anchor != null) {
            if (anchor == FIRST) {
                command.setPlaceFirst(true);
            }
            if (anchor == LAST) {
                command.setPlaceLast(true);
            }
            if (anchor == BEFORE) {
                command.setPlaceBefore(relativeId);
            }
            if (anchor == AFTER) {
                command.setPlaceAfter(relativeId);
            }
        }
        return command;
    }

}
