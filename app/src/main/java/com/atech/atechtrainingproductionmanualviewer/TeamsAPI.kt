package com.atech.atechtrainingproductionmanualviewer

import android.util.Log

// establish class for the notes/codes that are processed to teams
data class Note(val title: String, val content: String)


// TODO: THE TEAMS API FUNCTIONALITY HAS BEEN DEPRECATED AS OF AUG. 15 2024.
//  THIS VERSION 2.0 REMOVES THE TEAMS INTEGRATION TEMPORARILY.
//  TO RE-ENABLE, IMPLEMENT MICROSOFT GRAPH API WITH OAUTH2 AUTHENTICATION.
//  See: https://learn.microsoft.com/en-us/graph/api/channel-post-messages
class TeamsAPI {
    
    /**
     * [DEPRECATED] Sends the specified message/associated information [note], [trainer], [page] to Microsoft Teams
     * This function is non-functional as of August 15, 2024 due to Microsoft Teams Workflow retirement.
     * 
     * For v2.0, this method logs the attempt but does not send to Teams.
     * To restore functionality, implement Microsoft Graph API integration.
     */
    @Deprecated(
        "Microsoft Teams Workflow API retired. Implement Microsoft Graph API instead. " +
        "See https://learn.microsoft.com/en-us/graph/api/channel-post-messages",
        level = DeprecationLevel.WARNING
    )
    fun send(note: Note, trainer: String, page: String) {
        // Log the note instead of sending to Teams (temporary solution for v2.0)
        Log.i("TeamsAPI", "Note logged (Teams integration disabled): ${note.title} - ${note.content} - Trainer: $trainer, Page: $page")
        
        // Original Teams webhook functionality disabled
        // TODO: Implement Microsoft Graph API to re-enable Teams integration
        // https://learn.microsoft.com/en-us/graph/api/channel-post-messages
    }
}
