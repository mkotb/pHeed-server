package com.pheed.server.integration

import com.google.cloud.language.v1.Document
import com.google.cloud.language.v1.Entity
import com.google.cloud.language.v1.LanguageServiceClient
import com.pheed.server.model.StatusAnalysis
import com.pheed.server.model.TweetData
import com.pheed.server.model.UserAnalysis
import com.pheed.server.model.UserData
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.abs

object GoogleIntegration {
    val END_CHARACTERS = listOf('?', '.', '!')

    suspend fun performSentimentAnalysis(statuses: Map<User, List<Status>>): Map<String, UserAnalysis> {
        val collected = ConcurrentHashMap<String, UserAnalysis>()
        val coroutineContext = Executors.newFixedThreadPool(6).asCoroutineDispatcher()
        val jobs = ArrayList<Job>()

        statuses.forEach { (user, statuses) ->
            if (statuses.isEmpty()) {
                return@forEach
            }

            jobs.add (
                    CoroutineScope(coroutineContext).launch {
                        collected[user.screenName] = analyzeUser(user, statuses)
                    }
            )
        }

        jobs.forEach {
            it.join()
        }

        return collected
    }

    fun analyzeUser(user: User, statuses: List<Status>): UserAnalysis {
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
            val content = currentSentences.joinToString(" ")
                    .replace(" This sentence is a splitter. ", "")

            analyses.add(StatusAnalysis(
                    content,
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

        val originalEntities = client.analyzeEntitySentiment(document).entitiesList
        val entitySentiments = originalEntities
                .filter { abs(it.sentiment.score) > .35 }

        fun handleEntityList(list: List<Entity>): List<String> {
            return list.sortedByDescending { abs(it.sentiment.score) + (it.salience * 2) }
                    .map { it.name }.distinct()
        }

        client.close()

        return UserAnalysis(
                UserData(user.name, user.screenName, user.profileImageUrlHttps),
                analyses,
                handleEntityList(entitySentiments.filter { it.sentiment.score > 0 }),
                handleEntityList(entitySentiments.filter { it.sentiment.score < 0 })
        )
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

        text = text.replace("\n", " ")

        // strips off all non-ASCII characters
        text = text.replace("[^\\x00-\\x7F]".toRegex(), "")

        // erases all the ASCII control characters
        text = text.replace("[\\p{Cntrl}&&[^\r\n\t]]".toRegex(), "")

        // removes non-printable characters from Unicode
        text = text.replace("\\p{C}".toRegex(), "")

        text = text.trim()

        if (!END_CHARACTERS.contains(text.lastOrNull())) {
            text += '.'
        }

        return text
    }
}

