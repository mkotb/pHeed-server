package com.pheed.server.routes

import com.pheed.server.integration.GoogleIntegration
import com.pheed.server.integration.TwitterIntegration
import com.pheed.server.model.UserAnalysis
import io.javalin.http.Context
import kotlinx.coroutines.runBlocking

object AnalysisController {
    fun post(context: Context) {
        runBlocking {
            val request = context.bodyAsClass(AnalysisRequest::class.java)
            val others = TwitterIntegration.gatherFollowingTweets(request.token, request.tokenSecret)
            val self = TwitterIntegration.gatherOwnTweets(request.token, request.tokenSecret)

            context.json(AnalysisResponse(
                    GoogleIntegration.performSentimentAnalysis(others),
                    GoogleIntegration.performSentimentAnalysis(self)
            ))
        }
    }
}

data class AnalysisRequest(val token: String, val tokenSecret: String)
class AnalysisResponse(val analyses: Map<String, UserAnalysis>, val selfAnalysis: Map<String, UserAnalysis>)
