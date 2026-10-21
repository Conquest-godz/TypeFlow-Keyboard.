package com.typeflow.keyboard

object KeyboardLayout {
    val letters = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
        listOf("?123", "😀", ",", "SPACE", ".", "↵")
    )
    val numbers = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "\$", "%", "&", "-", "+", "(", ")"),
        listOf("=\\<", "*", "\"", "'", ":", ";", "!", "?", "⌫"),
        listOf("ABC", "😀", ",", "SPACE", ".", "↵")
    )

    val emojiCategories = linkedMapOf(
        "Recent" to listOf("😀", "😂", "🥰", "😍", "😭", "😊", "🔥", "❤️"),
        "Smileys" to listOf("😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "😉", "😊", "😍", "🥰", "😘", "😎", "🥳", "😭", "😡", "🤔", "🤗", "😴", "🤩", "💀"),
        "Gestures" to listOf("🙏", "👍", "🤝", "👏", "🫶", "💪", "👀", "🤞", "✌️", "👌", "👋", "🙌"),
        "Hearts" to listOf("❤️", "💖", "💔", "💕", "💗", "💓", "💘", "💝", "💞", "🖤", "🤍", "💜"),
        "Symbols" to listOf("🔥", "💯", "✨", "🎉", "⭐", "⚡", "🌈", "✅", "❌", "💡", "🎵", "🚀")
    )

    val emoji: List<List<String>> get() = emojiCategories["Smileys"]!!.map { "E:$it" }.chunked(8) +
        listOf(listOf("ABC", "CAT:Recent", "CAT:Smileys", "CAT:Gestures", "CAT:Hearts", "CAT:Symbols", "⌫"))

    val alternates = mapOf(
        "a" to listOf("á", "à", "â", "ä", "ã", "å", "æ"),
        "c" to listOf("ç", "ć", "č"),
        "e" to listOf("é", "è", "ê", "ë"),
        "i" to listOf("í", "ì", "î", "ï"),
        "n" to listOf("ñ"),
        "o" to listOf("ó", "ò", "ô", "ö", "õ", "ø", "œ"),
        "s" to listOf("ß", "ś", "š"),
        "u" to listOf("ú", "ù", "û", "ü"),
        "y" to listOf("ý", "ÿ"),
        "." to listOf("…", "。"),
        "," to listOf("،", "、"),
        "?" to listOf("¿", "؟"),
        "!" to listOf("¡"),
        "1" to listOf("¹", "½"),
        "2" to listOf("²", "⅔"),
        "-" to listOf("—", "–", "_"),
        "'" to listOf("’", "‘", "´")
    )
}
