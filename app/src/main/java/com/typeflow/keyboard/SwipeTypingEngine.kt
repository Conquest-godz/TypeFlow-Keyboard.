package com.typeflow.keyboard

import java.io.InputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Offline geometric glide decoder.
 *
 * First narrows the local dictionary using the approximate visited-key sequence, then ranks
 * candidates by the shape of the actual touch trace against the candidate's QWERTY key-centre
 * template using resampled, endpoint-anchored DTW. No gesture data leaves the device.
 */
internal object SwipeTypingEngine {
    data class Point(val x: Float, val y: Float)

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

    /** Compatibility fallback for unit tests and environments without coordinate samples. */
    fun recognize(path: List<Char>): String? {
        val normalized = normalizePath(path)
        if (normalized.length < 2) return null
        if (normalized in dictionary) return normalized
        val maxDistance = (normalized.length / 3).coerceIn(1, 3)
        return dictionary.asSequence()
            .filter { abs(it.length - normalized.length) <= maxDistance }
            .map { it to editDistance(normalized, it) }
            .filter { it.second <= maxDistance }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }
                .thenBy { if (it.first.firstOrNull() == normalized.first()) 0 else 1 }
                .thenBy { if (it.first.lastOrNull() == normalized.last()) 0 else 1 })
            .take(2).toList().let { ranked ->
                if (ranked.isEmpty()) null
                else if (ranked.size > 1 && ranked[0].second == ranked[1].second &&
                    ranked[0].first.length > 3) null
                else ranked[0].first
            }
    }

    fun recognizeSpatial(
        visitedKeys: List<Char>,
        rawPath: List<Point>,
        keyCenters: Map<Char, Point>
    ): String? {
        val pathKeys = normalizePath(visitedKeys)
        if (pathKeys.length < 2 || rawPath.size < 4 || keyCenters.size < 15) return null
        val startKey = nearestKey(rawPath.first(), keyCenters) ?: return null
        val endKey = nearestKey(rawPath.last(), keyCenters) ?: return null
        val normalizedPath = resample(rawPath, SAMPLE_COUNT)
        val maxEdit = (pathKeys.length / 3).coerceIn(1, 4)

        // Cheap lexical pruning prevents a full geometric comparison against every dictionary word.
        val candidates = dictionary.asSequence()
            .filter { word ->
                word.length in max(2, pathKeys.length - maxEdit)..(pathKeys.length + maxEdit) &&
                    word.first() == startKey && word.last() == endKey
            }
            .map { word -> word to editDistance(pathKeys, word) }
            .filter { it.second <= maxEdit }
            .sortedBy { it.second }
            .take(MAX_GEOMETRIC_CANDIDATES)
            .toList()

        if (candidates.isEmpty()) return recognize(pathKeys)
        val ranked = candidates.mapNotNull { (word, edits) ->
            val template = word.map { keyCenters[it] ?: return@mapNotNull null }
            val idealPath = resample(template, SAMPLE_COUNT)
            val geometry = bandedDtw(normalizedPath, idealPath)
            // Geometric fit dominates; lexical edit distance is only a small tie-breaker.
            word to (geometry + edits * EDIT_PENALTY)
        }.sortedBy { it.second }
        if (ranked.isEmpty()) return null
        val best = ranked[0]
        val runnerUp = ranked.getOrNull(1)
        // Reject poorly fitting paths and close ties rather than confidently inserting nonsense.
        if (best.second > MAX_ACCEPTED_SCORE) return null
        if (runnerUp != null && runnerUp.second - best.second < MIN_WIN_MARGIN &&
            best.first != pathKeys && best.second > LOW_CONFIDENCE_FLOOR) return null
        return best.first
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
            while (segment < cumulative.lastIndex - 1 && cumulative[segment] < target) segment++
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
        val band = max(4, abs(n - m) + 3)
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
    private const val MAX_GEOMETRIC_CANDIDATES = 100
    private const val EDIT_PENALTY = 0.006
    private const val MAX_ACCEPTED_SCORE = 0.30
    private const val MIN_WIN_MARGIN = 0.012
    private const val LOW_CONFIDENCE_FLOOR = 0.10
}
