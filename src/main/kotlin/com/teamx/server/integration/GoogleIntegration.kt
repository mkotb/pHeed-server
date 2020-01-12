package com.teamx.server.integration

import com.google.cloud.language.v1.Document
import com.google.cloud.language.v1.LanguageServiceClient
import com.google.cloud.language.v1.LanguageServiceSettings
import com.teamx.server.model.StatusAnalysis
import com.teamx.server.model.TweetData
import com.teamx.server.model.UserAnalysis
import com.teamx.server.model.UserData
import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.extensions.models.text
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.User

object GoogleIntegration {
    fun performSentimentAnalysis(statuses: Map<User, List<Status>>): Map<String, UserAnalysis> {
        val collected = HashMap<String, UserAnalysis>()

        statuses.forEach { (user, statuses) ->
            if (statuses.isEmpty()) {
                return@forEach
            }

            val client = LanguageServiceClient.create()
            val together = statuses.joinToString( " This sentence is a splitter. ", transform = this::cleanTweet)
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
            var index = 0

            fun pushCurrent() {
                analyses.add(StatusAnalysis(
                        currentSentences.joinToString(" "),
                        currentSentiment / currentSentences.size,
                        TweetData(statuses[index])
                ))

                currentSentiment = 0.0
                currentSentences.clear()

                index++
            }

            for (sentence in sentences) {
                if ("This sentence is a splitter." == sentence.text.content) {
                    pushCurrent()
                    continue
                }

                currentSentences.add(sentence.text.content)
                currentSentiment += sentence.sentiment.score
            }

            pushCurrent()

            collected[user.screenName] = UserAnalysis(
                    UserData(user.name, user.profileImageUrlHttps),
                    analyses
            )

            client.close()
        }

        return collected
    }

    fun cleanTweet(s: Status): String {
        var status = s

        if (status.retweetedStatus != null) {
            status = status.retweetedStatus!!
        }

        val original = status.fullTextRaw!!
        val toRemove = ArrayList<List<Int>>()

        status.entities.urls.forEach { e -> toRemove.add(e.indices) }
        status.entities.hashtags.forEach { e -> toRemove.add(e.indices) }
        status.entities.media.forEach { e -> toRemove.add(e.indices) }

        status.extendedEntities?.urls?.forEach { e -> toRemove.add(e.indices) }
        status.extendedEntities?.hashtags?.forEach {e -> toRemove.add(e.indices) }
        status.extendedEntities?.media?.forEach { e -> toRemove.add(e.indices) }

        val invalidRanges = toRemove.map { IntRange(it[0], it[1]) }
        val builder = StringBuilder()

        for (i in original.indices) {
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

        var text = builder.toString().trim()

        // strips off all non-ASCII characters
        text = text.replace("[^\\x00-\\x7F]".toRegex(), "")

        // erases all the ASCII control characters
        text = text.replace("[\\p{Cntrl}&&[^\r\n\t]]".toRegex(), "")

        // removes non-printable characters from Unicode
        text = text.replace("\\p{C}".toRegex(), "")

        text = text.trim()

        if (text.lastOrNull() != '.') {
            text += '.'
        }

        return text
    }
}

