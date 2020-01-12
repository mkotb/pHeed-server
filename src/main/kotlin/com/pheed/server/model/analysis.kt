package com.pheed.server.model

import jp.nephy.penicillin.extensions.likeCount
import jp.nephy.penicillin.models.Status

data class UserAnalysis (
        val userData: UserData,
        val analyses: List<StatusAnalysis>,
        val positiveEntities: List<String>,
        val negativeEntities: List<String>,
        val analysisStatistics: AnalysisStatistics = AnalysisStatistics(analyses),
        val overallScore: Double = analyses.map { it.score }.average()
)

fun findCount(analyses: List<StatusAnalysis>, low: Double, high: Double): Int {
    return analyses.count {
        it.score > low && it.score < high
    }
}

data class AnalysisStatistics (
        @Transient val analyses: List<StatusAnalysis>,
        val negativeStatusCount: Int = findCount(analyses, -1.0, -.2),
        val neutralStatusCount: Int = findCount(analyses, -.2, .2),
        val positiveStatusCount: Int = findCount(analyses, .2, 1.0),
        val negativePercentage: Int = ((negativeStatusCount.toDouble() / analyses.size) * 100).toInt(),
        val neutralPercentage: Int = ((neutralStatusCount.toDouble() / analyses.size) * 100).toInt(),
        val positivePercentage: Int = ((positiveStatusCount.toDouble() / analyses.size) * 100).toInt()
)

data class StatusAnalysis(val tweet: String, val score: Double, val tweetData: TweetData)
data class TweetData(@Transient val status: Status) {
    val createdAt = status.createdAtRaw
    val retweets = status.retweetCount
    val replies = status.replyCount
    val likes = status.likeCount
    val retweeted = status.retweeted
    val otherUserData: UserData? = run {
        val retweetStatus = status.retweetedStatus

        if (retweetStatus != null) {
            return@run UserData(retweetStatus.user.name, retweetStatus.user.screenName, retweetStatus.user.profileImageUrlHttps)
        }

        null
    }
}

data class UserData(val name: String, val username: String, val photoUrl: String)
