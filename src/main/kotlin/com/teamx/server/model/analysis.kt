package com.teamx.server.model

import jp.nephy.penicillin.extensions.likeCount
import jp.nephy.penicillin.models.Status

data class UserAnalysis (
        val userData: UserData,
        val analyses: List<StatusAnalysis>,
        val overallScore: Double = analyses.map { it.score }.average()
)

data class StatusAnalysis(val tweet: String, val score: Double, val tweetData: TweetData)
data class TweetData(@Transient val status: Status) {
    val createdAt = status.createdAtRaw
    val retweets = status.retweetCount
    val replies = status.replyCount
    val likes = status.likeCount
    val retweeted = status.retweeted
    val otherUserData: UserData? = run {
        if (status.retweeted) {
            val retweetStatus = status.retweetedStatus!!

            return@run UserData(retweetStatus.user.name, retweetStatus.user.profileImageUrlHttps)
        }

        null
    }
}

data class UserData(val name: String, val photoUrl: String)
