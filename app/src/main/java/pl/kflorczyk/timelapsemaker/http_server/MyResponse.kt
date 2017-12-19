package pl.kflorczyk.timelapsemaker.http_server

import org.json.JSONObject

/**
 * Created by Kamil on 2017-12-19.
 */
class MyResponse(status: ResponseStatus, description: String?, data: JSONObject?) {
    val status: String
    val description: String? = description
    val data: JSONObject? = data

    init {
        this.status = if(status == ResponseStatus.OK) "ok" else "fail"
    }

    fun build(): String {
        if(data?.has("status") == true || data?.has("description") == true || data?.has("data") == true)
            throw RuntimeException("Passing JSONObject must not have any key from 'status', 'description', 'data'")

        val o = JSONObject()
        o.put("status", status)
        o.put("description", description)
        o.put("data", data)
        return o.toString()
    }

    enum class ResponseStatus {
        OK,
        FAIL
    }
}
