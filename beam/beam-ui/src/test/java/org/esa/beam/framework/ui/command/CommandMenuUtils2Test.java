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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.esa.beam.framework.ui.command.CommandMenuUtils2Test.GroupAffiliation.Anchor.AFTER;
import static org.esa.beam.framework.ui.command.CommandMenuUtils2Test.GroupAffiliation.Anchor.BEFORE;
import static org.esa.beam.framework.ui.command.CommandMenuUtils2Test.GroupAffiliation.Anchor.FIRST;
import static org.esa.beam.framework.ui.command.CommandMenuUtils2Test.GroupAffiliation.Anchor.LAST;

public class CommandMenuUtils2Test {
    @Test
    public void testFirstOnly() {

        Command[] commands = toArray(new Command("a", new GroupAffiliation("x", FIRST, null)),
                                     new Command("b", new GroupAffiliation("x", FIRST, null)),
                                     new Command("c", new GroupAffiliation("x", FIRST, null)));
        shuffle(commands);

        Arrays.sort(commands, new CommandComparator());

        int index = 0;
        Assert.assertEquals("a", commands[index++].id);
        Assert.assertEquals("b", commands[index++].id);
        Assert.assertEquals("c", commands[index].id);
    }

    @Test
    public void testLastOnly() {

        Command[] commands = toArray(new Command("a", new GroupAffiliation("x", LAST, null)),
                                     new Command("b", new GroupAffiliation("x", LAST, null)),
                                     new Command("c", new GroupAffiliation("x", LAST, null)));
        shuffle(commands);

        Arrays.sort(commands, new CommandComparator());

        int index = 0;
        Assert.assertEquals("a", commands[index++].id);
        Assert.assertEquals("b", commands[index++].id);
        Assert.assertEquals("c", commands[index].id);
    }

    @Test
    public void testFirstAndLastOnly() {

        Command[] commands = toArray(new Command("a", new GroupAffiliation("x", FIRST, null)),
                                     new Command("b", new GroupAffiliation("x", FIRST, null)),
                                     new Command("c", new GroupAffiliation("x", FIRST, null)),
                                     new Command("d", new GroupAffiliation("x", LAST, null)),
                                     new Command("e", new GroupAffiliation("x", LAST, null)),
                                     new Command("f", new GroupAffiliation("x", LAST, null)));
        shuffle(commands);

        Arrays.sort(commands, new CommandComparator());

        int index = 0;
        Assert.assertEquals("a", commands[index++].id);
        Assert.assertEquals("b", commands[index++].id);
        Assert.assertEquals("c", commands[index++].id);
        Assert.assertEquals("d", commands[index++].id);
        Assert.assertEquals("e", commands[index++].id);
        Assert.assertEquals("f", commands[index].id);
    }

    @Test
    public void testAllKinds() {

        Command[] commands = toArray(new Command("a", new GroupAffiliation("x", FIRST, null)),
                                     new Command("b", new GroupAffiliation("x", FIRST, null)),
                                     new Command("c", new GroupAffiliation("x", FIRST, null)),
                                     new Command("d", new GroupAffiliation("x", AFTER, "c")),
                                     new Command("e", new GroupAffiliation("x", AFTER, "c")),
                                     new Command("f", new GroupAffiliation("x", AFTER, "c")),
                                     new Command("g", new GroupAffiliation("x", BEFORE, "k")),
                                     new Command("h", new GroupAffiliation("x", BEFORE, "k")),
                                     new Command("i", new GroupAffiliation("x", BEFORE, "k")),
                                     new Command("j", new GroupAffiliation("x", LAST, null)),
                                     new Command("k", new GroupAffiliation("x", LAST, null)),
                                     new Command("l", new GroupAffiliation("x", LAST, null)));
        shuffle(commands);

        Arrays.sort(commands, new CommandComparator());

        /*
        int index = 0;
        Assert.assertEquals("a", commands[index++].id);
        Assert.assertEquals("b", commands[index++].id);
        Assert.assertEquals("c", commands[index++].id);
        Assert.assertEquals("d", commands[index++].id);
        Assert.assertEquals("e", commands[index++].id);
        Assert.assertEquals("f", commands[index++].id);
        Assert.assertEquals("g", commands[index++].id);
        Assert.assertEquals("h", commands[index++].id);
        Assert.assertEquals("i", commands[index++].id);
        Assert.assertEquals("j", commands[index++].id);
        Assert.assertEquals("k", commands[index++].id);
        Assert.assertEquals("l", commands[index].id);
        */
    }

    private void shuffle(Command[] commands) {

        for (int i = 0; i < commands.length; i++) {
            int i1 = (int) (commands.length * Math.random());
            int i2 = (int) (commands.length * Math.random());
            Command command1 = commands[i1];
            Command command2 = commands[i2];
            commands[i1] = command2;
            commands[i2] = command1;
        }
    }

    Command[] toArray(Command... commands) {
        return commands;
    }

    public static class GroupAffiliation {
        public enum Anchor {
            FIRST,
            LAST,
            BEFORE,
            AFTER
        }

        public final String groupId;
        public final Anchor anchor;
        public final String relativeId;

        public GroupAffiliation(String groupId, Anchor anchor, String relativeId) {
            this.groupId = groupId;
            this.anchor = anchor;
            this.relativeId = relativeId;
        }
    }

    public static class Command {
        public final String id;
        public final GroupAffiliation groupAffiliation;

        public Command(String id, GroupAffiliation groupAffiliation) {
            this.id = id;
            this.groupAffiliation = groupAffiliation;
        }
    }

    public static class CommandComparator implements Comparator<Command> {
        @Override
        public int compare(Command c1, Command c2) {
            String id1 = c1.id;
            String id2 = c2.id;
            GroupAffiliation.Anchor anchor1 = c1.groupAffiliation.anchor;
            GroupAffiliation.Anchor anchor2 = c2.groupAffiliation.anchor;
            String relativeId1 = c1.groupAffiliation.relativeId;
            String relativeId2 = c2.groupAffiliation.relativeId;
            if (anchor1 == FIRST) {
                if (anchor2 == FIRST) {
                    return id1.compareTo(id2);
                } else {
                    return -1;
                }
            } else if (anchor1 == LAST) {
                if (anchor2 == LAST) {
                    return id1.compareTo(id2);
                } else {
                    return +1;
                }
            } else if (anchor1 == BEFORE) {
                if (anchor2 == FIRST) {
                    return -compare(c2, c1);
                } else if (anchor2 == LAST) {
                    return -compare(c2, c1);
                } else if (anchor2 == BEFORE) {
                    if (relativeId1.equals(relativeId2)) {
                        return id1.compareTo(id2);
                    } else if (relativeId1.equals(id2)) {
                        return +1;
                    } else {
                        return 0;
                    }
                } else if (anchor2 == AFTER) {
                    if (id1.equals(relativeId2)) {
                        return +1;
                    } else {
                        return 0;
                    }
                }
            } else if (anchor1 == AFTER) {
                if (anchor2 == FIRST) {
                    return -compare(c2, c1);
                } else if (anchor2 == LAST) {
                    return -compare(c2, c1);
                } else if (anchor2 == BEFORE) {
                    if (relativeId1.equals(id2)) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else if (anchor2 == AFTER) {
                    if (relativeId1.equals(relativeId2)) {
                        return id1.compareTo(id2);
                    } else if (id1.equals(relativeId2)) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }
            return 0;
        }
    }
}
