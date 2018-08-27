package com.fueledbysoda.todo

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.sql.SQLClient
import java.util.*
import java.util.stream.Collectors

data class TodoItem(
        val id: UUID = UUID.randomUUID(),
        var title: String,
        var completed: Boolean = false,
        val url: String = "${rootUrl()}/$id",
        var order: Int = -1) {
    constructor(json: JsonObject) : this(
            UUID.fromString(json.getString("id")),
            json.getString("title"),
            json.getBoolean("completed"),
            json.getString("url"),
            json.getInteger("order")
    )

//    constructor(json: JsonArray) : this(
//        json.getJsonObject(0)
//    )
}

class TodoService(private val sqlClient: SQLClient) {
    fun list(handler: ((AsyncResult<List<TodoItem>>) -> Unit)) {
        val sql = "SELECT * FROM Todo"

        sqlClient.query(sql) { result ->
            if (result.succeeded()) {
                handler(Future.succeededFuture(result.result().rows.stream().map { TodoItem(it) }.collect(Collectors.toList())))
            } else {
                handler(Future.failedFuture(result.cause()))
            }
        }
    }

    fun clear(handler: ((AsyncResult<List<TodoItem>>) -> Unit)) {
        val sql = "DELETE FROM Todo"

        sqlClient.update(sql) { result ->
            if (result.succeeded()) {
                handler(Future.succeededFuture(arrayListOf<TodoItem>()))
            } else {
                handler(Future.failedFuture(result.cause()))
            }
        }
    }

    fun add(input: TodoItem, handler: ((AsyncResult<TodoItem>) -> Unit)) {
        val item = TodoItem(title = input.title, order = input.order)

        val sql = "INSERT INTO Todo (id, title, completed, url, \"order\") VALUES (?, ?, ?, ?, ?)"
        val params = JsonArray().add(item.id.toString()).add(item.title).add(item.completed).add(item.url).add(item.order)

        sqlClient.updateWithParams(sql, params) { result ->
            if (result.succeeded()) {
                handler(Future.succeededFuture(item))
            } else {
                handler(Future.failedFuture(result.cause()))
            }
        }
    }

    fun get(id: UUID, handler: ((AsyncResult<TodoItem?>) -> Unit)) {
        val sql = "SELECT id, title, completed, url, \"order\" FROM Todo WHERE id = ?"
        val params = JsonArray().add(id.toString())

        sqlClient.querySingleWithParams(sql, params) { result ->
            if (result.succeeded()) {
                val jsonArray = result.result()

                val jsonObject = JsonObject()
                        .put("id", jsonArray.getString(0))
                        .put("title", jsonArray.getString(1))
                        .put("completed", jsonArray.getBoolean(2))
                        .put("url", jsonArray.getString(3))
                        .put("order", jsonArray.getInteger(4))

                handler(Future.succeededFuture(TodoItem(jsonObject)))
            } else {
                handler(Future.failedFuture(result.cause()))
            }
        }
    }

    fun patch(id: UUID, finalState: Map<String, Any>, handler: ((AsyncResult<TodoItem?>) -> Unit)) {
        // TODO: Construct sql & params from data in map for the case not all properties are being updated
        val sql = "UPDATE Todo SET title = ?, completed = ?, \"order\" = ? WHERE id = ?"
        val params = JsonArray().add(finalState["title"]).add(finalState["completed"]).add(finalState["order"]).add(id.toString())

        sqlClient.updateWithParams(sql, params) { result ->
            if (result.succeeded()) {
                get(id) { result ->
                    if (result.succeeded()) {
                        handler(Future.succeededFuture(result.result()))
                    } else {
                        handler(Future.failedFuture(result.cause()))
                    }
                }
            } else {
                handler(Future.failedFuture(result.cause()))
            }
        }
    }

    fun delete(id: UUID, handler: ((AsyncResult<UUID>) -> Unit)) {
        val sql = "DELETE FROM Todo WHERE id = ?"
        val params = JsonArray().add(id.toString())

        sqlClient.updateWithParams(sql, params) { result ->
            if (result.succeeded()) {
                handler(Future.succeededFuture(id))
            } else {
                handler(Future.failedFuture(result.cause()))
            }
        }
    }
}

