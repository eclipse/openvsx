/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation 
 *
 * See the NOTICE file(s) distributed with this work for additional 
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 
 ********************************************************************************/
package org.eclipse.openvsx.scanning;

import jakarta.validation.constraints.NotNull;
import java.util.*;

/**
 * Aho-Corasick string matching automaton for efficient multi-pattern search.
 * 
 * This implementation allows searching for thousands of keywords in linear time.
 * Instead of running hundreds of regexes on the entire file, we:
 * 1. Build a trie (prefix tree) from all keywords
 * 2. Add failure links for efficient state transitions
 * 3. Search the text once to find all keyword matches in O(n + m + z) time
 *    where n = text length, m = total pattern length, z = number of matches
 * 
 * Based on the algorithm used by TruffleHog for secret detection optimization.
 */
public class AhoCorasick {
    
    /**
     * Node in the Aho-Corasick trie.
     * Each node represents a state in the automaton.
     */
    private static class TrieNode {
        // Children nodes indexed by character
        Map<Character, TrieNode> children = new HashMap<>();
        
        // Failure link - where to go if no match found
        // This is the key to Aho-Corasick's efficiency
        TrieNode failure = null;
        
        // Output patterns at this node (if this node ends a pattern)
        List<String> outputs = new ArrayList<>();
    }
    
    private final TrieNode root;
    
    public AhoCorasick() {
        this.root = new TrieNode();
    }
    
    /**
     * Build the automaton from a collection of keywords.
     * This should be called once with all patterns before searching.
     * 
     * @param keywords Set of keywords to search for (should be lowercase for case-insensitive matching)
     */
    public void build(@NotNull Set<String> keywords) {
        // Step 1: Build the trie (prefix tree)
        // Add all keywords to the trie structure
        for (String keyword : keywords) {
            if (keyword == null || keyword.isEmpty()) {
                continue;
            }
            addKeyword(keyword);
        }
        
        // Step 2: Build failure links using BFS
        // Failure links allow us to continue matching after a mismatch
        // without backtracking in the text
        buildFailureLinks();
    }
    
    /**
     * Add a single keyword to the trie.
     */
    private void addKeyword(String keyword) {
        TrieNode current = root;
        
        for (char c : keyword.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
        }
        
        current.outputs.add(keyword);
    }
    
    /**
     * Build failure links for all nodes using BFS.
     * Failure links point to the longest proper suffix that is also a prefix of some pattern.
     * This is what makes Aho-Corasick efficient - we never backtrack in the input text.
     */
    private void buildFailureLinks() {
        Queue<TrieNode> queue = new LinkedList<>();
        
        // Initialize: all children of root fail back to root
        for (TrieNode child : root.children.values()) {
            child.failure = root;
            queue.add(child);
        }
        
        // BFS to build failure links for all nodes
        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();
            
            for (Map.Entry<Character, TrieNode> entry : current.children.entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();
                queue.add(child);
                
                // Find the failure link for this child
                // Walk up failure links until we find a node that has a child for 'c'
                TrieNode failNode = current.failure;
                while (failNode != null && !failNode.children.containsKey(c)) {
                    failNode = failNode.failure;
                }
                
                if (failNode == null) {
                    // No suffix match found, fail to root
                    child.failure = root;
                } else {
                    // Found a suffix match
                    child.failure = failNode.children.get(c);
                }
                
                // Copy outputs from failure node (for overlapping patterns)
                if (child.failure != null && child.failure != root) {
                    child.outputs.addAll(child.failure.outputs);
                }
            }
        }
    }
    
    /**
     * Search for all keyword matches in the given text.
     * Returns a list of matches with their positions.
     * 
     * @param text Text to search in (should be lowercase for case-insensitive matching)
     * @return List of all keyword matches found
     */
    public @NotNull List<Match> search(@NotNull String text) {
        List<Match> matches = new ArrayList<>();
        TrieNode current = root;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // Follow failure links until we find a match or reach root
            while (current != root && !current.children.containsKey(c)) {
                current = current.failure;
            }
            
            // Move to next state if possible
            if (current.children.containsKey(c)) {
                current = current.children.get(c);
            }
            
            // If this node has outputs, we found matches
            for (String keyword : current.outputs) {
                // Calculate the start position of the match
                int startPos = i - keyword.length() + 1;
                matches.add(new Match(keyword, startPos, i + 1));
            }
        }
        
        return matches;
    }
    
    /**
     * Represents a keyword match in the text.
     */
    public static class Match {
        private final String keyword;
        private final int startPos;
        private final int endPos;
        
        public Match(@NotNull String keyword, int startPos, int endPos) {
            this.keyword = keyword;
            this.startPos = startPos;
            this.endPos = endPos;
        }
        
        public @NotNull String getKeyword() {
            return keyword;
        }
        
        public int getStartPos() {
            return startPos;
        }
        
        public int getEndPos() {
            return endPos;
        }
        
        @Override
        public String toString() {
            return String.format("Match{keyword='%s', pos=[%d,%d)}", keyword, startPos, endPos);
        }
    }
}

