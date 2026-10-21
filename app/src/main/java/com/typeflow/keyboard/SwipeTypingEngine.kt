package com.typeflow.keyboard

import java.io.InputStream
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Offline geometric glide decoder. Candidate generation is bounded, the touch trace is scored
 * against local QWERTY templates, and low-confidence traces are rejected rather than guessed.
 * No gesture data leaves the device.
 */
internal object SwipeTypingEngine {
    data class Point(val x: Float, val y: Float)
    private data class Candidate(val word: String, val edits: Int)
    private data class RankedWord(val word: String, val score: Double)

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
                .distinct()
                .toList()
            if (loaded.isNotEmpty()) dictionary = (loaded + seedWords).distinct()
        }
    }

    fun normalizePath(keys: List<Char>): String = keys.asSequence()
        .map { it.lowercaseChar() }
        .filter { it in 'a'..'z' }
        .fold(StringBuilder()) { acc, c -> if (acc.lastOrNull() != c) acc.append(c) else acc }
        .toString()

    /** Legacy lexical helper; production glide recognition uses recognizeSpatial only. */
    fun recognize(path: List<Char>): String? {
        val normalized = normalizePath(path)
        if (normalized.length < 2) return null
        if (normalized in dictionary) return normalized
        val maxDistance = (normalized.length / 3).coerceIn(1, 3)
        val ranked = dictionary.asSequence()
            .filter { abs(it.length - normalized.length) <= maxDistance }
            .map { it to editDistance(normalized, it) }
            .filter { it.second <= maxDistance }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }
                .thenBy { if (it.first.firstOrNull() == normalized.first()) 0 else 1 }
                .thenBy { if (it.first.lastOrNull() == normalized.last()) 0 else 1 }
                .thenBy { it.first })
            .take(2).toList()
        if (ranked.isEmpty()) return null
        if (ranked.size > 1 && ranked[0].second == ranked[1].second && ranked[0].first.length > 3) return null
        return ranked[0].first
    }

    fun recognizeSpatial(
        visitedKeys: List<Char>,
        rawPath: List<Point>,
        keyCenters: Map<Char, Point>
    ): String? {
        if (visitedKeys.size < 2 || rawPath.size < 4 || keyCenters.size < MIN_KEY_CENTERS) return null
        val pathKeys = normalizePath(visitedKeys)
        if (pathKeys.length < 2) return null
        val startKey = nearestKey(rawPath.first(), keyCenters) ?: return null
        val endKey = nearestKey(rawPath.last(), keyCenters) ?: return null
        val normalizedPath = resample(rawPath, SAMPLE_COUNT)

        // Consecutive-key collapse loses repeated letters; raw visit count keeps those words in range.
        val minLength = max(2, min(pathKeys.length, visitedKeys.size) - 3)
        val maxLength = max(pathKeys.length, visitedKeys.size) + 6
        val maxEdit = (pathKeys.length / 2).coerceIn(2, 8)

        // Try strict endpoints first, but retry when the strict result is absent OR fails confidence.
        val strict = collectCandidates(startKey, endKey, minLength, maxLength, pathKeys, maxEdit, true)
        decodeCandidates(strict, normalizedPath, keyCenters, pathKeys)?.let { return it }

        val tolerant = collectCandidates(startKey, endKey, minLength, maxLength, pathKeys, maxEdit, false)
        return decodeCandidates(tolerant, normalizedPath, keyCenters, pathKeys)
    }

    private fun decodeCandidates(
        candidates: List<Candidate>,
        normalizedPath: List<Point>,
        keyCenters: Map<Char, Point>,
        pathKeys: String
    ): String? {
        if (candidates.isEmpty()) return null
        val ranked = candidates.mapNotNull { candidate ->
            val template = candidate.word.map { keyCenters[it] ?: return@mapNotNull null }
            val idealPath = resample(template, SAMPLE_COUNT)
            val geometry = bandedDtw(normalizedPath, idealPath)
            RankedWord(candidate.word, geometry + candidate.edits * EDIT_PENALTY)
        }.sortedWith(compareBy<RankedWord> { it.score }.thenBy { it.word })
        if (ranked.isEmpty()) return null
        val best = ranked[0]
        val runnerUp = ranked.getOrNull(1)
        if (!best.score.isFinite() || best.score > MAX_ACCEPTED_SCORE) return null
        if (runnerUp != null &&
            runnerUp.score - best.score < MIN_WIN_MARGIN &&
            best.word != pathKeys &&
            best.score > LOW_CONFIDENCE_FLOOR
        ) return null
        return best.word
    }

    /**
     * Keep only the best lexical candidates in a bounded heap. Sequence.sortedBy().take(k)
     * still sorts the entire eligible dictionary, so it does not bound selection cost.
     */
    private fun collectCandidates(
        startKey: Char,
        endKey: Char,
        minLength: Int,
        maxLength: Int,
        pathKeys: String,
        maxEdit: Int,
        enforceEndpoints: Boolean
    ): List<Candidate> {
        val worstFirst = compareByDescending<Candidate> { it.edits }.thenByDescending { it.word }
        val heap = PriorityQueue(worstFirst)
        val limit = MAX_GEOMETRIC_CANDIDATES
        for (word in dictionary) {
            if (word.length !in minLength..maxLength) continue
            if (enforceEndpoints && (word.first() != startKey || word.last() != endKey)) continue
            val edits = editDistance(pathKeys, word)
            if (edits > max(maxEdit, word.length / 2)) continue
            val candidate = Candidate(word, edits)
            if (heap.size < limit) {
                heap.add(candidate)
            } else if (worstFirst.compare(candidate, heap.peek()) > 0) {
                heap.poll()
                heap.add(candidate)
            }
        }
        return heap.sortedWith(compareBy<Candidate> { it.edits }.thenBy { it.word })
    }

    private fun nearestKey(point: Point, centers: Map<Char, Point>): Char? =
        centers.minByOrNull { (_, center) -> distance(point, center) }?.key

    private fun resample(points: List<Point>, count: Int): List<Point> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return List(count) { points.first() }
        val cumulative = DoubleArray(points.size)
        for (i in 1 until points.size) cumulative[i] = cumulative[i - 1] + distance(points[i - 1], points[i])
        val total = cumulative.last()
        if (total < 1e-5) return List(count) { points.first() }
        val result = ArrayList<Point>(count)
        var segment = 1
        for (i in 0 until count) {
            val target = total * i / (count - 1).coerceAtLeast(1)
            // Advance through the final segment too. Stopping at lastIndex - 1 silently
            // truncated every resampled path before its actual endpoint, weakening endpoint
            // evidence for words such as "hello" vs "help".
            while (segment < cumulative.lastIndex && cumulative[segment] < target) segment++
            val before = cumulative[segment - 1]
            val span = (cumulative[segment] - before).coerceAtLeast(1e-8)
            val t = ((target - before) / span).coerceIn(0.0, 1.0).toFloat()
            val a = points[segment - 1]
            val b = points[segment]
            result.add(Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
        }
        return result
    }

    private fun bandedDtw(a: List<Point>, b: List<Point>): Double {
        if (a.isEmpty() || b.isEmpty()) return Double.POSITIVE_INFINITY
        val n = a.size
        val m = b.size
        val band = max(6, abs(n - m) + 4)
        var previous = DoubleArray(m + 1) { Double.POSITIVE_INFINITY }
        previous[0] = 0.0
        for (i in 1..n) {
            val current = DoubleArray(m + 1) { Double.POSITIVE_INFINITY }
            val from = max(1, i - band)
            val to = min(m, i + band)
            for (j in from..to) {
                val local = distance(a[i - 1], b[j - 1])
                val endpointWeight = if (i == 1 || i == n) 1.5 else 1.0
                current[j] = local * endpointWeight + minOf(previous[j], current[j - 1], previous[j - 1])
            }
            previous = current
        }
        return previous[m] / (n + m)
    }

    private fun distance(a: Point, b: Point): Double =
        hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())

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

    private const val SAMPLE_COUNT = 24
    private const val MAX_GEOMETRIC_CANDIDATES = 300
    private const val MIN_KEY_CENTERS = 15
    private const val EDIT_PENALTY = 0.006
    private const val MAX_ACCEPTED_SCORE = 0.30
    private const val MIN_WIN_MARGIN = 0.012
    private const val LOW_CONFIDENCE_FLOOR = 0.10
}