package com.teamx.server.integration

import com.google.cloud.language.v1.Document
import com.google.cloud.language.v1.LanguageServiceClient
import com.google.cloud.language.v1.LanguageServiceSettings
import com.teamx.server.model.StatusAnalysis
import com.teamx.server.model.UserAnalysis
import jp.nephy.penicillin.extensions.models.text
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.User

object GoogleIntegration {
    fun performSentimentAnalysis(statuses: Map<User, List<Status>>): Map<String, UserAnalysis> {
        val collected = HashMap<String, UserAnalysis>()
        val client = LanguageServiceClient.create()

        statuses.forEach { (user, statuses) ->
            val together = statuses.joinToString("\n SPLIT \n", transform = this::cleanTweet)
            val document = Document.newBuilder().apply {
                content = together
                type = Document.Type.PLAIN_TEXT
            }.build()
            val analyses = ArrayList<StatusAnalysis>()
            val sentences = client.analyzeSentiment (
                    document
            ).sentencesList

            val currentSentences = ArrayList<String>()
            var currentSentiment = 0.0

            fun pushCurrent() {
                analyses.add(StatusAnalysis(
                        currentSentences.joinToString(" "),
                        currentSentiment / currentSentences.size
                ))

                currentSentiment = 0.0
                currentSentences.clear()
            }

            for (sentence in sentences) {
                if ("SPLIT" == sentence.text.content) {
                    pushCurrent()
                    continue
                }

                currentSentences.add(sentence.text.content)
                currentSentiment += sentence.sentiment.score
            }

            pushCurrent()

            collected[user.screenName] = UserAnalysis(
                    user.name,
                    user.profileImageUrlHttps,
                    analyses
            )
        }

        client.close()

        return collected
    }

    private fun cleanTweet(status: Status): String {
        val original = status.text
        val toRemove = ArrayList<List<Int>>()

        status.entities.hashtags.forEach { e -> toRemove.add(e.indices)}
        status.entities.symbols.forEach { e -> toRemove.add(e.indices)}
        status.entities.urls.forEach { e -> toRemove.add(e.indices) }
        status.entities.userMentions.forEach { e -> toRemove.add(e.indices) }

        val invalidRanges = toRemove.map { IntRange(it[0], it[1]) }
        val builder = StringBuilder()

        for (i in 0..original.length) {
            if (invalidRanges.any { it.contains(i) }) {
                continue
            }

            val char = original[i]

            // ignore preceding spaces (or extra spaces)
            if (char == ' ') {
                val last = builder.lastOrNull()

                if (last == null || last == ' ') {
                    continue
                }
            }

            builder.append(char)
        }

        return builder.toString().trim()
    }
}

