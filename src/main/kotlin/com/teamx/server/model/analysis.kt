package com.teamx.server.model

data class UserAnalysis (
        val name: String,
        val photoUrl: String,
        val analyses: List<StatusAnalysis>,
        val overallScore: Double = analyses.sumByDouble { it.score }
)

data class StatusAnalysis(val tweet: String, val score: Double)
