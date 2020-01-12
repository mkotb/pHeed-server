package com.teamx.server.integration

import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.session.config.account
import jp.nephy.penicillin.core.session.config.application
import jp.nephy.penicillin.core.session.config.token
import jp.nephy.penicillin.endpoints.common.TweetMode
import jp.nephy.penicillin.endpoints.friends
import jp.nephy.penicillin.endpoints.friends.listUsers
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.userTimeline
import jp.nephy.penicillin.endpoints.timeline.userTimelineByUserId
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.User

object TwitterIntegration {
    suspend fun gatherFollowingTweets(accessToken: String, accessTokenSecret: String): Map<User, List<Status>> {
        val client = PenicillinClient {
            account {
                application(System.getenv("TWITTER_KEY"), System.getenv("TWITTER_SECRET"))
                token(accessToken, accessTokenSecret)
            }
        }

        val users = client.friends.listUsers.await().result.users

        return users.associateWith { user ->
            client.timeline.userTimelineByUserId(user.id, count = 15, tweetMode = TweetMode.Extended).await()
                    .results
        }
    }

    suspend fun gatherOwnTweets(accessToken: String, accessTokenSecret: String): Map<User, List<Status>> {
        val client = PenicillinClient {
            account {
                application(System.getenv("TWITTER_KEY"), System.getenv("TWITTER_SECRET"))
                token(accessToken, accessTokenSecret)
            }
        }

        val statuses = client.timeline.userTimeline(count = 15, tweetMode = TweetMode.Extended).await().results

        if (statuses.isEmpty()) {
            return emptyMap()
        }

        return HashMap<User, List<Status>>().apply {
            this[statuses.first { !it.retweeted }.user] = statuses
        }
    }
}