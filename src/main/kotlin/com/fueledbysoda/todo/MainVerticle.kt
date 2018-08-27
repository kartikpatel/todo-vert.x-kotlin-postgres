package com.fueledbysoda.todo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.asyncsql.PostgreSQLClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import java.util.*

fun port(): Int {
    return System.getenv("PORT")?.toInt() ?: 8080
}

fun rootUrl(): String {
    return System.getenv("HEROKU_APP_NAME")?.let { "https://$it.herokuapp.com" }
            ?: "http://localhost:${port()}"
}

class MainVerticle : AbstractVerticle() {
    @Throws(Exception::class)
    override fun start() {
        val postgreSQLClientConfig = JsonObject()
                .put("host", "localhost")
                .put("port", 5432)
                .put("maxPoolSize", 10)
                .put("username", "postgres")
                .put("database", "todo_dev")
        val postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig)
        val todoService = TodoService(postgreSQLClient)

        val router = createRouter(todoService)

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port())
    }

    private fun createRouter(todoService: TodoService): Router {
        val mapper = ObjectMapper().registerModule(KotlinModule())
        val router = Router.router(vertx)

        router.route().handler(
                CorsHandler.create("*")
                        .allowedMethod(HttpMethod.GET)
                        .allowedMethod(HttpMethod.POST)
                        .allowedMethod(HttpMethod.OPTIONS)
                        .allowedMethod(HttpMethod.DELETE)
                        .allowedMethod(HttpMethod.PATCH)
                        .allowedHeader("X-PINGARUNNER")
                        .allowedHeader("Content-Type")
        )

        router.route().handler(BodyHandler.create())

        router.route("/").handler { ctx ->
            ctx.response().putHeader("content-type", "application/json")
            ctx.next()
        }

        router.options("/").handler { ctx ->
            ctx.response().end()
        }

        router.get("/").handler { ctx ->
            todoService.list { ar ->
                if (ar.succeeded()) {
                    ctx.response().end(Json.encode(ar.result()))
                } else {
                    ctx.fail(ar.cause())
                }
            }
        }

        router.get("/:id").handler { ctx ->
            todoService.get(UUID.fromString(ctx.pathParam("id"))) { ar ->
                if (ar.succeeded()) {
                    ar.result()?.let { ctx.response().end(Json.encode(it)) } ?: run { ctx.fail(404) }
                } else {
                    ctx.fail(ar.cause())
                }
            }
        }

        router.patch("/:id").handler { ctx ->
            val map = mapper.readValue<Map<String, Any>>(ctx.bodyAsString)
            todoService.patch(UUID.fromString(ctx.pathParam("id")), map) { ar ->
                if (ar.succeeded()) {
                    ar.result()?.let { ctx.response().end(Json.encode(it)) } ?: run { ctx.fail(404) }
                } else {
                    ctx.fail(ar.cause())
                }
            }
        }

        router.post("/").handler { ctx ->
            val todoItem = mapper.readValue<TodoItem>(ctx.bodyAsString)

            todoService.add(todoItem) { ar ->
                if (ar.succeeded()) {
                    ctx.response().end(Json.encode(ar.result()))
                } else {
                    ctx.fail(ar.cause())
                }
            }
        }

        router.delete("/").handler { ctx ->
            todoService.clear { ar ->
                if (ar.succeeded()) {
                    ctx.response().end(Json.encode(ar.result()))
                } else {
                    ctx.fail(ar.cause())
                }
            }
        }

        router.delete("/:id").handler { ctx ->
            todoService.delete(UUID.fromString(ctx.pathParam("id"))) {ar ->
                if (ar.succeeded()) {
                    ctx.response().end(Json.encode(ar.result()))
                } else {
                    ctx.fail(ar.cause())
                }
            }
        }

        return router
    }
}