package com.findit.service;

import com.findit.model.Report;

import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Offline intelligent matching engine between LOST and FOUND reports.
 * Uses a weighted multi-factor heuristic algorithm combining Category, Levenshtein
 * name distance, keyword token overlap, location string matching, and date proximity.
 */
public class SmartMatchingService {

    public static class MatchResult implements Comparable<MatchResult> {
        private final Report report;
        private final int score;
        private final List<String> reasons;

        public MatchResult(Report report, int score, List<String> reasons) {
            this.report = report;
            this.score = score;
            this.reasons = reasons;
        }

        public Report getReport() {
            return report;
        }

        public int getScore() {
            return score;
        }

        public List<String> getReasons() {
            return reasons;
        }

        @Override
        public int compareTo(MatchResult o) {
            return Integer.compare(o.score, this.score); // Descending order
        }
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "in", "on", "at", "by", "for", "with", "about",
            "against", "between", "into", "through", "during", "before", "after",
            "above", "below", "to", "from", "up", "down", "is", "am", "are",
            "was", "were", "be", "been", "being", "have", "has", "had", "do",
            "does", "did", "and", "but", "if", "or", "because", "as", "until",
            "while", "of", "my", "your", "his", "her", "its", "our", "their", "this", "that"
    );

    /**
     * Finds potential matches for the given target report among a list of candidates.
     * Evaluates opposite report types (LOST matches FOUND and vice-versa) with minimum score threshold.
     */
    public static List<MatchResult> findMatches(Report target, List<Report> allReports, int minScore) {
        if (target == null || allReports == null || allReports.isEmpty()) {
            return Collections.emptyList();
        }

        String targetType = target.getReportType() != null ? target.getReportType().toUpperCase() : "";
        String oppositeType = "LOST".equals(targetType) ? "FOUND" : "LOST";

        List<MatchResult> results = new ArrayList<>();

        for (Report candidate : allReports) {
            if (candidate.getReportId() == target.getReportId()) {
                continue;
            }

            // Only match against opposite type
            String candType = candidate.getReportType() != null ? candidate.getReportType().toUpperCase() : "";
            if (!oppositeType.equals(candType)) {
                continue;
            }

            // Skip returned items
            String candStatus = candidate.getStatus() != null ? candidate.getStatus().toUpperCase() : "";
            if ("RETURNED".equals(candStatus)) {
                continue;
            }

            MatchResult match = calculateMatch(target, candidate);
            if (match.getScore() >= minScore) {
                results.add(match);
            }
        }

        Collections.sort(results);
        return results;
    }

    /**
     * Calculates weighted similarity score between two reports (0 to 100).
     */
    public static MatchResult calculateMatch(Report target, Report candidate) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // 1. Category Match (Weight: 30 points)
        if (target.getItem() != null && candidate.getItem() != null) {
            int tCat = target.getItem().getCategoryId();
            int cCat = candidate.getItem().getCategoryId();
            String tCatName = target.getItem().getCategoryName();
            String cCatName = candidate.getItem().getCategoryName();

            if (tCat > 0 && tCat == cCat) {
                score += 30;
                reasons.add("Category Match: " + (tCatName != null ? tCatName : "Same Category") + " (+30%)");
            } else if (tCatName != null && cCatName != null && tCatName.equalsIgnoreCase(cCatName)) {
                score += 30;
                reasons.add("Category Match: " + tCatName + " (+30%)");
            }
        }

        // 2. Item Name Similarity (Weight: 30 points)
        String tName = target.getItem() != null && target.getItem().getItemName() != null ? target.getItem().getItemName() : "";
        String cName = candidate.getItem() != null && candidate.getItem().getItemName() != null ? candidate.getItem().getItemName() : "";

        double nameSim = computeStringSimilarity(tName, cName);
        if (nameSim > 0.3) {
            int namePts = (int) Math.round(nameSim * 30);
            score += namePts;
            reasons.add("Name Similarity (" + Math.round(nameSim * 100) + "% match, +" + namePts + "%)");
        }

        // 3. Description Keyword Overlap (Weight: 20 points)
        String tDesc = target.getItem() != null && target.getItem().getDescription() != null ? target.getItem().getDescription() : "";
        String cDesc = candidate.getItem() != null && candidate.getItem().getDescription() != null ? candidate.getItem().getDescription() : "";

        double keywordSim = computeTokenOverlap(tDesc, cDesc);
        if (keywordSim > 0.15) {
            int kwPts = (int) Math.round(keywordSim * 20);
            score += kwPts;
            reasons.add("Keyword Overlap in Description (+" + kwPts + "%)");
        }

        // 4. Location Proximity (Weight: 10 points)
        String tLoc = target.getLocation() != null ? target.getLocation() : "";
        String cLoc = candidate.getLocation() != null ? candidate.getLocation() : "";

        double locSim = computeStringSimilarity(tLoc, cLoc);
        if (locSim > 0.15) {
            int locPts = Math.max(3, (int) Math.round(locSim * 10));
            score += locPts;
            reasons.add("Location Proximity: " + cLoc + " (+" + locPts + "%)");
        }

        // 5. Date Proximity (Weight: 10 points)
        if (target.getReportDate() != null && candidate.getReportDate() != null) {
            long daysApart = Math.abs(ChronoUnit.DAYS.between(target.getReportDate(), candidate.getReportDate()));
            if (daysApart <= 3) {
                score += 10;
                reasons.add("Reported within " + daysApart + " day(s) (+10%)");
            } else if (daysApart <= 7) {
                score += 8;
                reasons.add("Reported within 1 week (+8%)");
            } else if (daysApart <= 14) {
                score += 5;
                reasons.add("Reported within 2 weeks (+5%)");
            } else if (daysApart <= 21) {
                score += 2;
                reasons.add("Reported within 3 weeks (+2%)");
            }
        }

        // Cap at 100
        score = Math.min(100, score);
        return new MatchResult(candidate, score, reasons);
    }

    /**
     * Computes similarity between two strings combining Levenshtein Distance & Token Jaccard index.
     */
    public static double computeStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String str1 = s1.trim().toLowerCase();
        String str2 = s2.trim().toLowerCase();

        if (str1.isEmpty() && str2.isEmpty()) return 1.0;
        if (str1.isEmpty() || str2.isEmpty()) return 0.0;
        if (str1.equals(str2)) return 1.0;

        // Levenshtein ratio
        int maxLen = Math.max(str1.length(), str2.length());
        int dist = computeLevenshteinDistance(str1, str2);
        double levRatio = 1.0 - ((double) dist / maxLen);

        // Token Jaccard ratio
        double jaccardRatio = computeTokenOverlap(str1, str2);

        return Math.max(levRatio, jaccardRatio);
    }

    private static double computeTokenOverlap(String s1, String s2) {
        Set<String> set1 = tokenize(s1);
        Set<String> set2 = tokenize(s2);

        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null) return tokens;

        String[] words = text.toLowerCase().split("[^a-zA-Z0-9]+");
        for (String w : words) {
            if (w.length() > 2 && !STOP_WORDS.contains(w)) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    private static int computeLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }
}
