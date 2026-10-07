/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.osgi.test.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-safe in-memory store for messages captured by TestWsLogHandler.
 * Provides query, verification against routerThing patterns, and reset capabilities.
 */
public class CapturedMessageStore {

    public static class CapturedEntry {
        private final String formattedMessage;
        private final String messageId;
        private final boolean messageHidden;
        private final long timestamp;

        public CapturedEntry(String formattedMessage, boolean messageHidden) {
            this.formattedMessage = formattedMessage;
            this.messageHidden = messageHidden;
            this.timestamp = System.currentTimeMillis();
            this.messageId = parseMessageId(formattedMessage);
        }

        public String getFormattedMessage() {
            return formattedMessage;
        }

        public String getMessageId() {
            return messageId;
        }

        public boolean isMessageHidden() {
            return messageHidden;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return formattedMessage;
        }
    }

    public static class VerificationResult {
        private final boolean passed;
        private final String routerThing;
        private final int totalCaptured;
        private final List<String> matchedMessages;
        private final List<String> unmatchedMessages;

        public VerificationResult(boolean passed, String routerThing, int totalCaptured,
                                  List<String> matchedMessages, List<String> unmatchedMessages) {
            this.passed = passed;
            this.routerThing = routerThing;
            this.totalCaptured = totalCaptured;
            this.matchedMessages = matchedMessages;
            this.unmatchedMessages = unmatchedMessages;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getRouterThing() {
            return routerThing;
        }

        public int getTotalCaptured() {
            return totalCaptured;
        }

        public List<String> getMatchedMessages() {
            return matchedMessages;
        }

        public List<String> getUnmatchedMessages() {
            return unmatchedMessages;
        }
    }

    private static final List<CapturedEntry> entries = Collections.synchronizedList(new ArrayList<CapturedEntry>());

    /**
     * Add a captured message entry to the store.
     */
    public static void addMessage(String formattedMessage, boolean messageHidden) {
        if (formattedMessage != null) {
            entries.add(new CapturedEntry(formattedMessage, messageHidden));
        }
    }

    /**
     * Get a snapshot copy of all captured entries.
     */
    public static List<CapturedEntry> getEntries() {
        synchronized (entries) {
            return new ArrayList<CapturedEntry>(entries);
        }
    }

    /**
     * Get a snapshot copy of all formatted message strings.
     */
    public static List<String> getCapturedMessages() {
        synchronized (entries) {
            List<String> list = new ArrayList<String>(entries.size());
            for (CapturedEntry e : entries) {
                list.add(e.getFormattedMessage());
            }
            return list;
        }
    }

    /**
     * Get the count of captured messages.
     */
    public static int getCount() {
        return entries.size();
    }

    /**
     * Clear all captured messages from memory.
     */
    public static void clear() {
        entries.clear();
    }

    /**
     * Verifies all captured messages in memory against a routerThing pattern configuration.
     * routerThing can be comma-separated list of exact IDs or wildcard patterns (e.g. ABCD*, ABCD*I, CWWKF0011I).
     */
    public static VerificationResult verifyAgainst(String routerThing) {
        List<CapturedEntry> snapshot = getEntries();
        List<String> matched = new ArrayList<String>();
        List<String> unmatched = new ArrayList<String>();

        String[] rawPatterns = (routerThing != null) ? routerThing.split(",") : new String[0];
        List<String> patterns = new ArrayList<String>();
        for (String p : rawPatterns) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                patterns.add(trimmed);
            }
        }

        for (CapturedEntry entry : snapshot) {
            String msgId = entry.getMessageId();
            if (matchesAnyPattern(msgId, patterns)) {
                matched.add(entry.getFormattedMessage());
            } else {
                unmatched.add(entry.getFormattedMessage());
            }
        }

        boolean passed = unmatched.isEmpty();
        return new VerificationResult(passed, routerThing, snapshot.size(), matched, unmatched);
    }

    /**
     * Checks if a message ID matches any of the given patterns.
     */
    public static boolean matchesAnyPattern(String msgId, List<String> patterns) {
        if (msgId == null || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (matchesPattern(msgId, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Matches a message ID against a single pattern (exact, prefix*, or prefix*Level).
     */
    public static boolean matchesPattern(String msgId, String pattern) {
        if (msgId == null || pattern == null) {
            return false;
        }
        pattern = pattern.trim();
        if (pattern.equals("*")) {
            return true;
        }

        int asteriskIdx = pattern.indexOf('*');
        if (asteriskIdx == -1) {
            // Exact message ID match
            return msgId.equals(pattern);
        }

        // Wildcard pattern: <prefix>*[<level>]
        String prefix = pattern.substring(0, asteriskIdx);
        if (!msgId.startsWith(prefix)) {
            return false;
        }

        if (asteriskIdx == pattern.length() - 1) {
            // Pattern ends with '*', matches any level
            return true;
        } else if (asteriskIdx == pattern.length() - 2) {
            // Pattern has a level char after '*' (e.g. ABCD*I)
            char expectedLevel = pattern.charAt(pattern.length() - 1);
            if (!msgId.isEmpty()) {
                char actualLevel = msgId.charAt(msgId.length() - 1);
                return Character.toUpperCase(actualLevel) == Character.toUpperCase(expectedLevel);
            }
        }

        return false;
    }

    /**
     * Extracts the message ID from a formatted message (first 10 chars, trimmed to colon if 9-char TRAS).
     */
    public static String parseMessageId(String formattedMessage) {
        if (formattedMessage == null) {
            return "";
        }
        String trimmed = formattedMessage.trim();
        if (trimmed.length() >= 10) {
            String msgId = trimmed.substring(0, 10);
            if (msgId.endsWith(":")) {
                msgId = msgId.substring(0, msgId.indexOf(":"));
            }
            int spaceIdx = msgId.indexOf(' ');
            if (spaceIdx > 0) {
                msgId = msgId.substring(0, spaceIdx);
            }
            return msgId.trim();
        } else if (trimmed.contains(":")) {
            return trimmed.substring(0, trimmed.indexOf(":")).trim();
        } else if (trimmed.contains(" ")) {
            return trimmed.substring(0, trimmed.indexOf(" ")).trim();
        }
        return trimmed;
    }
}
