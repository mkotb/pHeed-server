package com.pheed.server

import com.google.gson.GsonBuilder
import com.pheed.server.routes.AnalysisController
import com.pheed.server.routes.ErrorException
import com.pheed.server.routes.ErrorResponse
import com.pheed.server.routes.InvalidRequestError
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.Context
import io.javalin.plugin.json.FromJsonMapper
import io.javalin.plugin.json.JavalinJson
import io.javalin.plugin.json.ToJsonMapper
import java.lang.reflect.Modifier

val gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
        .disableHtmlEscaping()
        .create()


fun main() {
    initServer(8080)
}

fun initServer(port: Int): Javalin {
    val app = Javalin.create()
            .start(port)

    val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    JavalinJson.fromJsonMapper = object : FromJsonMapper {
        override fun <T> map(json: String, targetClass: Class<T>): T {
            return moshi.adapter(targetClass)!!.fromJson(json)!!
        }
    }

    JavalinJson.toJsonMapper = object : ToJsonMapper {
        override fun map(obj: Any): String {
            return gson.toJson(obj)
        }
    }

    fun handleError(error: ErrorResponse, context: Context) {
        context.status(error.httpCode ?: 400)
        context.json(error)
    }

    app.exception(JsonDataException::class.java) { ex, context ->
        handleError(InvalidRequestError(ex.message
                ?: "JSON did not match request schema"), context)
    }

    app.exception(JsonEncodingException::class.java) { _, context ->
        handleError(InvalidRequestError("Invalid JSON"), context)
    }

    app.exception(ErrorException::class.java) { ex, context ->
        handleError(ex.error, context)
    }

    app.exception(Exception::class.java) { ex, context ->
        ex.printStackTrace()
        handleError(ErrorResponse(231, 231, ex.message!!), context)
    }

    app.routes {
        path("analysis") {
            post(AnalysisController::post)
        }
    }

    return app
}
