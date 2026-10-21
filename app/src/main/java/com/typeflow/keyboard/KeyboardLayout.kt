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

    // Full Unicode Emoji 16.0 dataset lives in UnicodeEmojiData and is shown in one scrollable palette.
    val alternates = mapOf(
        "q" to listOf("1", "q", "Q"),
        "w" to listOf("2", "ŵ", "ẃ", "ẁ", "ẅ"),
        "e" to listOf("3", "é", "è", "ê", "ë", "ē", "ė", "ę"),
        "r" to listOf("4", "ŕ", "ř"),
        "t" to listOf("5", "ť", "þ"),
        "y" to listOf("6", "ý", "ÿ", "ŷ"),
        "u" to listOf("7", "ú", "ù", "û", "ü", "ū", "ů"),
        "i" to listOf("8", "í", "ì", "î", "ï", "ī", "į"),
        "o" to listOf("9", "ó", "ò", "ô", "ö", "õ", "ø", "œ", "ō"),
        "p" to listOf("0", "þ", "¶"),
        "a" to listOf("@", "á", "à", "â", "ä", "ã", "å", "æ", "ā", "ą"),
        "s" to listOf("ß", "ś", "š", "ş"),
        "d" to listOf("ð", "ď"),
        "f" to listOf("ƒ"),
        "g" to listOf("ğ", "ģ"),
        "h" to listOf("ħ"),
        "j" to listOf("ĵ"),
        "k" to listOf("ķ"),
        "l" to listOf("ł", "ľ"),
        "z" to listOf("ž", "ź", "ż"),
        "x" to listOf("×"),
        "c" to listOf("ç", "ć", "č", "©"),
        "v" to listOf("✓"),
        "b" to listOf("β"),
        "n" to listOf("ñ", "ń"),
        "m" to listOf("µ"),
        "." to listOf("…", "。", "·"),
        "," to listOf("،", "、", "¿"),
        "?" to listOf("¿", "؟"),
        "!" to listOf("¡"),
        "1" to listOf("¹", "½", "⅓"),
        "2" to listOf("²", "⅔"),
        "3" to listOf("³", "¾"),
        "4" to listOf("¼"),
        "5" to listOf("⅝"),
        "6" to listOf("⅞"),
        "0" to listOf("°"),
        "-" to listOf("—", "–", "_"),
        "'" to listOf("’", "‘", "´"),
        "\$" to listOf("€", "£", "¥", "₹")
    )
}
