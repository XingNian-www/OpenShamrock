package moe.fuqiuluo.shamrock.remote.api

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import moe.fuqiuluo.shamrock.remote.action.handlers.CleanCache
import moe.fuqiuluo.shamrock.remote.action.handlers.DownloadFile
import moe.fuqiuluo.shamrock.remote.action.handlers.GetDeviceBattery
import moe.fuqiuluo.shamrock.remote.action.handlers.GetVersionInfo
import moe.fuqiuluo.shamrock.remote.action.handlers.RestartMe
import moe.fuqiuluo.shamrock.remote.entries.Status
import moe.fuqiuluo.shamrock.remote.service.config.ShamrockConfig
import moe.fuqiuluo.shamrock.tools.fetchOrNull
import moe.fuqiuluo.shamrock.tools.fetchOrThrow
import moe.fuqiuluo.shamrock.tools.getOrPost
import moe.fuqiuluo.shamrock.tools.respond
import moe.fuqiuluo.shamrock.utils.FileUtils
import moe.fuqiuluo.shamrock.utils.MD5

fun Routing.otherAction() {

    getOrPost("/get_version_info") {
        call.respondText(GetVersionInfo())
    }

    getOrPost("/get_device_battery") {
        call.respondText(GetDeviceBattery())
    }

    getOrPost("/clean_cache") {
        call.respondText(CleanCache())
    }

    getOrPost("/set_restart") {
        call.respondText(RestartMe(2000))
    }

    getOrPost("/download_file") {
        val url = fetchOrNull("url")
        val b64 = fetchOrNull("base64")
        val threadCnt = fetchOrNull("thread_cnt")?.toInt() ?: 0
        val headers = fetchOrNull("headers") ?: ""
        call.respondText(DownloadFile(url, b64, threadCnt, headers.split("\r\n")))
    }

    post("/upload_file") {
        val partData = call.receiveMultipart()
        partData.forEachPart { part ->
            if (part.name == "file") {
                val bytes = (part as PartData.FileItem).streamProvider().readBytes()
                val tmp = FileUtils.renameByMd5(FileUtils.getTmpFile("cache").also {
                    it.writeBytes(bytes)
                })
                respond(true, Status.Ok, DownloadFile.DownloadResult(
                    file = tmp.absolutePath,
                    md5 = MD5.genFileMd5Hex(tmp.absolutePath)
                ), "成功")
                return@forEachPart
            }
        }
        respond(false, Status.BadRequest, "没有上传文件信息")
    }

    getOrPost("/config/set_boolean") {
        val key = fetchOrThrow("key")
        val value = fetchOrThrow("value").toBooleanStrict()
        ShamrockConfig[key] = value
        respond(true, Status.Ok, "success")
    }

    getOrPost("/config/set_int") {
        val key = fetchOrThrow("key")
        val value = fetchOrThrow("value").toInt()
        ShamrockConfig[key] = value
        respond(true, Status.Ok, "success")
    }

    getOrPost("/config/set_string") {
        val key = fetchOrThrow("key")
        val value = fetchOrThrow("value")
        ShamrockConfig[key] = value
        respond(true, Status.Ok, "success")
    }
}