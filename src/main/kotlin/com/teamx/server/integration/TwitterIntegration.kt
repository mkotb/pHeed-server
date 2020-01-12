package com.teamx.server.integration

import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.session.config.account
import jp.nephy.penicillin.core.session.config.application
import jp.nephy.penicillin.core.session.config.token
import jp.nephy.penicillin.endpoints.friends
import jp.nephy.penicillin.endpoints.friends.listUsers
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.userTimelineByUserId
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.User

object TwitterIntegration {
    suspend fun gatherFollowingTweets(accessToken: String): Map<User, List<Status>> {
        val client = PenicillinClient {
            account {
                application("ConsumerKey", System.getenv("TWITTER_APP"))
                token("AccessToken", accessToken)
            }
        }

        val users = client.friends.listUsers.await().result.users

        return users.associateWith { user ->
            client.timeline.userTimelineByUserId(user.id, count = 50).await()
                    .results
        }
    }
}