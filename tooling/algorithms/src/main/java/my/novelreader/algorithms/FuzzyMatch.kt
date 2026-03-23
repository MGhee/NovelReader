package my.novelreader.algorithms

/**
 * Levenshtein distance-based fuzzy matching utilities for search suggestions.
 */

/**
 * Calculates the Levenshtein distance between two strings (case-insensitive).
 * Lower distance = strings are more similar.
 */
fun levenshteinDistance(a: String, b: String): Int {
    val aLower = a.lowercase()
    val bLower = b.lowercase()
    val aLen = aLower.length
    val bLen = bLower.length

    val dp = Array(aLen + 1) { IntArray(bLen + 1) }

    for (i in 0..aLen) dp[i][0] = i
    for (j in 0..bLen) dp[0][j] = j

    for (i in 1..aLen) {
        for (j in 1..bLen) {
            val cost = if (aLower[i - 1] == bLower[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,      // deletion
                dp[i][j - 1] + 1,      // insertion
                dp[i - 1][j - 1] + cost // substitution
            )
        }
    }

    return dp[aLen][bLen]
}

/**
 * Calculates a fuzzy match score between a query and a candidate (0.0 to 1.0).
 * 1.0 = exact match or substring match
 * 0.0 = completely different
 * Fast path: if candidate contains query (case-insensitive), returns 1.0
 */
fun fuzzyScore(query: String, candidate: String): Double {
    if (query.isEmpty()) return 1.0
    if (candidate.isEmpty()) return 0.0

    val queryLower = query.lowercase()
    val candidateLower = candidate.lowercase()

    // Fast path: exact substring match
    if (candidateLower.contains(queryLower)) {
        return 1.0
    }

    val maxLen = maxOf(queryLower.length, candidateLower.length)
    val distance = levenshteinDistance(queryLower, candidateLower)

    return 1.0 - (distance.toDouble() / maxLen.toDouble())
}

/**
 * Sorts a list by fuzzy match score against a query.
 * Filters items below the threshold, then sorts by descending score.
 */
fun <T> List<T>.sortedByFuzzyMatch(
    query: String,
    selector: (T) -> String,
    threshold: Double = 0.3
): List<T> {
    if (query.isBlank()) return this

    return this
        .map { item -> item to fuzzyScore(query, selector(item)) }
        .filter { (_, score) -> score >= threshold }
        .sortedByDescending { (_, score) -> score }
        .map { (item, _) -> item }
}
