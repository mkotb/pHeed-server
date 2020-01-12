package com.pheed.server.routes

import com.pheed.server.integration.GoogleIntegration
import com.pheed.server.integration.TwitterIntegration
import com.pheed.server.model.UserAnalysis
import io.javalin.http.Context
import kotlinx.coroutines.runBlocking

object AnalysisController {
    fun post(context: Context) {
        println("Received request to analyze")

        runBlocking {
            val request = context.bodyAsClass(AnalysisRequest::class.java)
            val others = TwitterIntegration.gatherFollowingTweets(request.token, request.tokenSecret, request.count)
            val self = TwitterIntegration.gatherOwnTweets(request.token, request.tokenSecret)

            println("Processed Twitter")

            context.json(AnalysisResponse(
                    GoogleIntegration.performSentimentAnalysis(others),
                    GoogleIntegration.performSentimentAnalysis(self)
            ))

            println("Finished")
        }
    }
}

data class AnalysisRequest(val token: String, val tokenSecret: String, val count: Int = 50)
class AnalysisResponse(val analyses: Map<String, UserAnalysis>, val selfAnalysis: Map<String, UserAnalysis>)
