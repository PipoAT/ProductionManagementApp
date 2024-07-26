package com.atech.atechtrainingproductionmanualviewer

import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
// establish class for the notes/codes that are processed to teams
data class Note(val title: String, val content: String)


// TODO: THE TEAMS API WILL NOT WORK AS OF AUG. 15 2024 DUE TO RETIREMENT OF THE NEEDED MICROSOFT TEAMS FUNCTIONALITY.
//  PLEASE FIX SO THAT IT IS NOT TIED TO APIPO@ATECHTRAINING.COM AND THAT IT ACTUALLY SENDS THE BELOW DATA.
//  THE CURRENT URL IS TIED TO A WORKFLOW THAT ONLY SENDS A GENERAL MESSAGE
// https://learn.microsoft.com/en-us/power-automate/?utm_source=flow-sidebar&utm_medium=web
class TeamsAPI {
    @DelicateCoroutinesApi
    /**
     * Sends the specified message/associated information [note], [trainer], [page] to Microsoft Teams
     * or sends a 6-digit code to Microsoft Teams
     */
    fun send(note: Note, trainer: String, page: String) {
        val teamsWebhookUrl = "https://prod-100.westus.logic.azure.com:443/workflows/2fa95a41970341879c89001557853861/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=zlieAyFrzyiPpAQWtUgX6OBLSLw1VOofRFUo4pVs5ms"
        // obtains the current date and time plus 1 week for other tasks via specified format
        val dueDate = LocalDateTime.now().plusWeeks(1)
        // generates a random 6 digit code
        GlobalScope.launch(Dispatchers.IO) {
            // creates the message format/adds information into message format to send
            val messageCardNew = mapOf(
                "type" to "message",
                "attachments" to listOf(
                    mapOf(
                        "contentType" to "application/vnd.microsoft.card.adaptive",
                        "contentUrl" to null,
                        "content" to mapOf(
                            "\$schema" to "http://adaptivecards.io/schemas/adaptive-card.json",
                            "type" to "AdaptiveCard",
                            "version" to "1.2",
                            "body" to listOf(
                                mapOf(
                                    "type" to "TextBlock",
                                    "text" to "${note.title} ${note.content}"
                                ),
                                mapOf(
                                    "type" to "FactSet",
                                    "facts" to listOf(
                                        mapOf("title" to "Trainer/Document", "value" to trainer),
                                        mapOf("title" to "Page Number", "value" to page),
                                        mapOf("title" to "Due Date", "value" to dueDate)
                                    )
                                )
                            )
                        )
                    )
                )
            )


            // create the request to send to Teams
            val request = Request.Builder().url(teamsWebhookUrl).post(Gson().toJson(messageCardNew).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())).build()
            try {
                // check for a valid response
                OkHttpClient().newCall(request).execute()
                // error message to post onto app if all fails
            } catch (e: Exception) { println(e) }
        }
    }
}