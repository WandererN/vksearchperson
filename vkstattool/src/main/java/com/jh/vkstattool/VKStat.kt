package com.jh.vkstattool

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.net.ServerSocket
import java.net.URI

class VKStat {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(VKStat::class.java)
        const val APP_ID = -1;
        const val CLIENT_SECRET = ""
        const val REDIRECT_URI = ""

        @JvmStatic
        fun main(args: Array<String>) {
            val transportClient = OkHttpTransportClient()
            val vk = VkApiClient(transportClient)
            val credFile = File("cred")
            val userId: Int
            val token: String
            if (credFile.exists()) {
                val auth = credFile.readText().split("\n")
                token = auth[0]
                userId = auth[1].toInt()
            } else {
                Desktop.getDesktop().browse(
                    URI(
                        "https://oauth.vk.com/authorize?client_id=$APP_ID" +
                                "&redirect_uri=$REDIRECT_URI&display=page&scope=friends" +
                                "&response_type=code"
                    )
                )
                var res = ""
                ServerSocket(3311).apply {
                    accept().use {
                        var finish = false
                        while (!finish) {
                            val c = it.getInputStream().read()
                            if (c == -1)
                                break
                            res += c.toChar()
                            finish = res.endsWith("\r\n\r\n")
                        }
                        log.info(res)
                    }
                }

                val code = res.split("/?code=")[1].split(" ")[0]
                log.info("code = $code")
                val authRespond = vk.oAuth()
                    .userAuthorizationCodeFlow(APP_ID, CLIENT_SECRET, REDIRECT_URI, code)
                    .execute()
                if (authRespond.error == null)
                    File("cred").writeText("${authRespond.accessToken}\n${authRespond.userId}")
                userId = authRespond.userId
                token = authRespond.accessToken

            }
            val actor = UserActor(userId, token)
            val personFinder = VkPersonFinder(vk, actor)
            val crossUsers = arrayListOf(
                415287789,
                4208583)
            log.info(
                "Common friends of: " + personFinder.getUsersByIds(crossUsers.toSet())
                    .joinToString(", ") { "${it.firstName} ${it.lastName}" })

            val crossGroups = arrayListOf(73581821)
            log.info("Searching common members of groups: " +
                    personFinder.getGroupsByIds(crossGroups.toSet())
                        .joinToString("\n") { "${it.id} ${it.name}" })
            log.info(
                personFinder.findUsersWithFullInfo(crossUsers, crossGroups)
                    .joinToString("\n") { "${it.id}: ${it.firstName} ${it.lastName}" })
        }
    }
}
