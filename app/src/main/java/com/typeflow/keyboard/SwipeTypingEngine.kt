package com.typeflow.keyboard

/**
 * Small offline recognizer foundation. It maps the visited key sequence to the
 * closest word in a compact built-in lexicon; no path or text leaves the device.
 * A full language model is intentionally not implied by this compact engine.
 */
internal object SwipeTypingEngine {
    private val words = setOf(
        "a", "about", "after", "again", "all", "also", "am", "an", "and", "are", "as", "at",
        "be", "because", "been", "but", "by", "can", "come", "could", "day", "do", "down",
        "for", "from", "get", "go", "good", "had", "has", "have", "he", "her", "here", "him",
        "his", "how", "i", "if", "in", "into", "is", "it", "just", "know", "like", "make",
        "me", "more", "my", "new", "no", "not", "now", "of", "on", "one", "or", "our", "out",
        "people", "please", "right", "say", "see", "she", "so", "some", "take", "than", "that",
        "the", "their", "them", "then", "there", "these", "they", "think", "this", "time", "to",
        "up", "us", "use", "very", "want", "was", "way", "we", "well", "what", "when", "which",
        "who", "will", "with", "would", "yes", "you", "your", "hello", "world", "thanks", "love"
    )

    fun normalizePath(keys: List<Char>): String = keys.asSequence()
        .map { it.lowercaseChar() }
        .filter { it in 'a'..'z' }
        .fold(StringBuilder()) { acc, c -> if (acc.lastOrNull() != c) acc.append(c) else acc }
        .toString()

    fun recognize(path: List<Char>): String? {
        val normalized = normalizePath(path)
        if (normalized.length < 2) return null
        if (normalized in words) return normalized
        return words.asSequence()
            .filter { it.firstOrNull() == normalized.first() && it.lastOrNull() == normalized.last() }
            .map { it to editDistance(normalized, it) }
            .filter { (_, distance) -> distance <= maxOf(1, normalized.length / 3) }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    private fun editDistance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            previous = current
        }
        return previous[b.length]
    }
}
