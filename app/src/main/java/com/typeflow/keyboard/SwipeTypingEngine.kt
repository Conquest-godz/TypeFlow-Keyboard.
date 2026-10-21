package com.typeflow.keyboard

import java.io.InputStream

/**
 * Offline swipe recognizer backed by a local English word list.
 * Only visited key labels are considered; no typed text or gesture path leaves the device.
 */
internal object SwipeTypingEngine {
    private val seedWords = setOf(
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
    @Volatile private var dictionary: List<String> = seedWords.toList()

    fun loadDictionary(input: InputStream) {
        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            val loaded = lines.map { it.trim().lowercase() }
                .filter { it.length in 2..32 && it.all { c -> c in 'a'..'z' } }
                .toList()
            if (loaded.isNotEmpty()) dictionary = (loaded + seedWords).distinct()
        }
    }

    fun normalizePath(keys: List<Char>): String = keys.asSequence()
        .map { it.lowercaseChar() }
        .filter { it in 'a'..'z' }
        .fold(StringBuilder()) { acc, c -> if (acc.lastOrNull() != c) acc.append(c) else acc }
        .toString()

    fun recognize(path: List<Char>): String? {
        val normalized = normalizePath(path)
        if (normalized.length < 2) return null
        val words = dictionary
        if (normalized in words) return normalized

        // Glide paths commonly miss intermediate keys, so permit a small endpoint mismatch
        // as well as insertions/deletions. A confidence ceiling prevents wild guesses.
        val maxDistance = when {
            normalized.length <= 4 -> 2
            normalized.length <= 7 -> 2
            else -> 3
        }
        var bestWord: String? = null
        var bestScore = Double.POSITIVE_INFINITY
        var secondBestScore = Double.POSITIVE_INFINITY
        for (word in words) {
            if (kotlin.math.abs(word.length - normalized.length) > maxDistance) continue
            val distance = editDistance(normalized, word)
            if (distance > maxDistance) continue
            val endpointPenalty =
                (if (word.firstOrNull() == normalized.first()) 0.0 else 0.85) +
                (if (word.lastOrNull() == normalized.last()) 0.0 else 0.85)
            // Prefer exact endpoints, modest length differences, and the shortest edit path.
            val score = distance.toDouble() + endpointPenalty +
                kotlin.math.abs(word.length - normalized.length) * 0.16
            if (score < bestScore) {
                secondBestScore = bestScore
                bestScore = score
                bestWord = word
            } else if (score < secondBestScore) {
                secondBestScore = score
            }
        }
        // Don't turn an uncertain glide into a random dictionary word.
        if (bestScore > maxDistance + 0.9) return null
        if (secondBestScore.isFinite() && secondBestScore - bestScore < 0.12 && bestScore > 1.0) {
            return null
        }
        return bestWord
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
