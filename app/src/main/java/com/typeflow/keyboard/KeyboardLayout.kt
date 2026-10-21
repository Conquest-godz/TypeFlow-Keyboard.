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

    // Compact, local emoji picker. Each item is inserted as ordinary text through
    // the active InputConnection; no clipboard or network access is involved.
    val emoji = listOf(
        listOf("E:😀", "E:😃", "E:😄", "E:😁", "E:😆", "E:😅", "E:🤣", "E:😂"),
        listOf("E:🙂", "E:🙃", "E:😉", "E:😊", "E:😍", "E:🥰", "E:😘", "E:😎"),
        listOf("E:🥳", "E:😭", "E:😡", "E:🤔", "E:🤗", "E:🙏", "E:👍", "E:❤️"),
        listOf("E:🔥", "E:💯", "E:✨", "E:🎉", "E:💀", "E:🤝", "E:👏", "E:💖"),
        listOf("ABC", "E:🫶", "E:💔", "E:😴", "E:🤩", "E:💪", "E:👀", "⌫")
    )
}
