package com.atech.atechtrainingproductionmanualviewer

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
// establish class for the notes/codes that are processed to teams
data class Note(val title: String, val content: String)


// TODO: THE TEAMS API FUNCTIONALITY HAS BEEN DEPRECATED AS OF AUG. 15 2024.
//  THIS VERSION 2.0 REMOVES THE TEAMS INTEGRATION TEMPORARILY.
//  TO RE-ENABLE, IMPLEMENT MICROSOFT GRAPH API WITH OAUTH2 AUTHENTICATION.
//  See: https://learn.microsoft.com/en-us/graph/api/channel-post-messages
class TeamsAPI {
    // Use application-scoped coroutine context instead of deprecated GlobalScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * [DEPRECATED] Sends the specified message/associated information [note], [trainer], [page] to Microsoft Teams
     * This function is non-functional as of August 15, 2024 due to Microsoft Teams Workflow retirement.
     * 
     * For v2.0, this method logs the attempt but does not send to Teams.
     * To restore functionality, implement Microsoft Graph API integration.
     */
    @Deprecated("Microsoft Teams Workflow API retired. Use Microsoft Graph API instead.")
    fun send(note: Note, trainer: String, page: String) {
        // Log the note instead of sending to Teams (temporary solution for v2.0)
        android.util.Log.i("TeamsAPI", "Note logged (Teams integration disabled): ${note.title} - ${note.content} - Trainer: $trainer, Page: $page")
        
        // Original Teams webhook functionality disabled
        // TODO: Implement Microsoft Graph API to re-enable Teams integration
        // https://learn.microsoft.com/en-us/graph/api/channel-post-messages
    }
}
