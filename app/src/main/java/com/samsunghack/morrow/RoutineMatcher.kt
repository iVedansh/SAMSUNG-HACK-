package com.samsunghack.morrow

data class RoutineMatch(val routine: Routine, val score: Int)

object RoutineMatcher {
    fun match(command: String, routines: List<Routine>): RoutineMatch? {
        val words = tokenize(command)
        if (words.isEmpty()) return null

        return routines
            .map { RoutineMatch(it, score(words, it)) }
            .filter { it.score >= 2 }
            .maxByOrNull { it.score }
    }

    private fun score(command: Set<String>, routine: Routine): Int {
        val phraseWords = tokenize(routine.phrase)
        val nameWords = tokenize(routine.name)
        val overlap = command.count { it in phraseWords || it in nameWords }
        val exactPhrase = normalize(routine.phrase).let {
            normalize(command.joinToString(" ")).contains(it)
        }
        return overlap + if (exactPhrase) 3 else 0
    }

    private fun tokenize(value: String): Set<String> =
        normalize(value).split(Regex("\\s+"))
            .filter { it.length > 2 }
            .toSet()

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
