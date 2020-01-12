package com.teamx.server.routes

import com.teamx.server.integration.GoogleIntegration
import com.teamx.server.integration.TwitterIntegration
import com.teamx.server.model.UserAnalysis
import io.javalin.http.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object AnalysisController {
    fun post(context: Context) {
        GlobalScope.launch {
            val request = context.bodyAsClass(AnalysisRequest::class.java)
            val tweets = TwitterIntegration.gatherFollowingTweets(request.twitterToken)

            context.json(AnalysisResponse(GoogleIntegration.performSentimentAnalysis(
                    tweets
            )))
        }
    }
}

data class AnalysisRequest(val twitterToken: String)
class AnalysisResponse(val analyses: Map<String, UserAnalysis>)