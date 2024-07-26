package com.atech.atechtrainingproductionmanualviewer

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.atech.atechtrainingproductionmanualviewer.DatabaseDbHelper.NotesTable
import com.atech.atechtrainingproductionmanualviewer.MainActivity.IDM.darkModeBtn
import com.atech.atechtrainingproductionmanualviewer.MainActivity.IDM.isDarkMode
import com.atech.atechtrainingproductionmanualviewer.databinding.ActivityNoteBinding
import com.atech.atechtrainingproductionmanualviewer.databinding.DialogNoteInteractionBinding
import com.atech.atechtrainingproductionmanualviewer.databinding.ListItemNoteBinding.inflate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

/**
 * The NoteAdapter class decomposes the notes from its original format and separate into the notes list
 * seen on the notes part of the app. It is broken down via a specified regex patter
 */
class NoteAdapter(context: Context, resource: Int, private val notes: List<String>) :
    ArrayAdapter<String>(context, resource, notes) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // inflate the layout from its context
        val listItemNote = inflate(LayoutInflater.from(context))
        // decompose the note based on specific formatting
        val (username, icons, content, trainer, pdfNameWithPage) = Regex("^(.*?):\\s*([^\\-]*)\\s*-\\s*(.*?)\\s*(?:\\((.*?)\\s*(.*?)\\))?$").find(notes[position])!!.destructured
        // add the decomposed parts of the note into its respective text fields on the layout
        listItemNote.noteUsername.text = username
        listItemNote.noteIcons.text = icons.ifBlank { "-" }
        listItemNote.noteTrainer.text = if (pdfNameWithPage.contains("Trainer")) "-" else "$trainer ${pdfNameWithPage.substringBefore(".pdf")} Page${pdfNameWithPage.substringAfter(".pdf")}"
        listItemNote.noteContent.text = if (content.length > 50) "${content.take(49)}..." else content
        // return the completed layout
        return listItemNote.root
    }
}

@DelicateCoroutinesApi
class NoteActivity : AppCompatActivity() {

    companion object { const val ACTION_CLOSE_NOTE_ACTIVITY = "com.example.app.ACTION_CLOSE_NOTE_ACTIVITY" } // define object for calling note activity to close

    private var dbHelper: DatabaseDbHelper = DatabaseDbHelper(this) // initialize the database handler
    private var currentPage = 1 // set initial current page to 1
    private var totalPages = 1 // set initial total page to 1
    private var imageUriString = "" // initialize the uri string
    private lateinit var activityNote: ActivityNoteBinding
    private val takePictureContract = registerForActivityResult(ActivityResultContracts.TakePicture()) {} // initialize camera launcher
    private val closeNoteActivityReceiver = object : BroadcastReceiver() { // broadcast receiver to close the note activity from a different part of the app
        override fun onReceive(context: Context, intent: Intent) { if (intent.action == ACTION_CLOSE_NOTE_ACTIVITY) { finishAndRemoveTask() } }
    }
    override fun onCreate(savedInstanceState: Bundle?) { // creates the layout/inflate
        super.onCreate(savedInstanceState)
        registerReceiver(closeNoteActivityReceiver, IntentFilter(ACTION_CLOSE_NOTE_ACTIVITY), null, null, Context.RECEIVER_EXPORTED)

        activityNote = ActivityNoteBinding.inflate(LayoutInflater.from(this))
        setContentView(activityNote.root)

        // calls function to open the new note dialog when user clicks on new note button
        activityNote.openDialogButton.setOnClickListener { openNewNoteDialog() }

        activityNote.backButton.setOnClickListener {// close note activity if user clicks on close button
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_HISTORY)
            finishAndRemoveTask() // close task
        }

        updateNotesListView() // calls function to update the notes list to display what is on the current page
        // initializes the dropdown for users filtering
        activityNote.userSpinner.apply {
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { updateNotesListView() }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            // Create the list of options starting with the default option
            val usernameList = mutableListOf("All Notes")
            // Query the database to get the list of usernames
            DatabaseDbHelper(context).readableDatabase.query(true, NotesTable.TABLE_NAME, arrayOf(NotesTable.NOTE_OWNER),
                null, null, null, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) { cursor.getString(cursor.getColumnIndexOrThrow(NotesTable.NOTE_OWNER))?.takeIf { it.isNotBlank() }?.let { usernameList.add(it) } }
            }
            // Set the adapter for the spinner
            adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, usernameList) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    // Set text color to black for- the spinner item
                    (view as TextView).setTextColor(-0x1000000)
                    return view
                }
            }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }

        // Set up click listeners for the page control buttons
        activityNote.backArrow.setOnClickListener {
            if (currentPage > 1) {
                --currentPage
                updateNotesListView()
            }
        }
        activityNote.forwardArrow.setOnClickListener {
            if (currentPage < totalPages) {
                ++currentPage
                updateNotesListView()
            }
        }
    }

    /**
     * Sets up and opens the dialog for a user to enter in a new note
     */
    private fun openNewNoteDialog() {
        // get the different elements from the layout
        DialogNoteInteractionBinding.inflate(LayoutInflater.from(this)).apply {
            counterTextview.text = resources.getQuantityString(R.plurals.character_count, 0, 0, 150)
            createTextWatcher(this)
            setupDropdown(this, "Select Trainer")
            // creates the save button to save a new note
            AlertDialog.Builder(this@NoteActivity)
                .setView(this.root)
                .setPositiveButton("Save") { _, _ -> saveNote(this, "New") }
                // call function to set up add photo button functionality
                .setNegativeButton("Add Photo") { _, _ -> setupAddPhotoButton(this) }
                .setNeutralButton("Dismiss") { dialog, _ -> dialog.dismiss() } // set the dismiss button to close out of new note dialog
                .setCancelable(false) // set dialog to where user cannot escape by clicking outside
                .create()
                .apply {
                    btnCancel.setOnClickListener { dismiss() } // set the action to close out dialog
                    show()
                    // Customize button text color after dialog is shown
                    if (isDarkMode(this@NoteActivity)) { darkModeBtn(this@NoteActivity, this) }
                }
        }
    }

    /**
     * Returns a generated uri  associated with a newly taken image while saving a [note] and associated trainer [selectedTrainer]
     * and page number [selectedPage] into the database
     */
    private fun createImageUri(username: String, note: String, selectedTrainer: String, selectedPage: String): Uri? {
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "note_image/${username}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }) // generate the image uri from photo

        // save the note with image uri into database
        dbHelper.writableDatabase.execSQL(
            "INSERT INTO ${NotesTable.TABLE_NAME} (${NotesTable.NOTE_OWNER},${NotesTable.COLUMN_IMAGE_URI}, ${NotesTable.COLUMN_NOTE}, ${NotesTable.COLUMN_TRAINER}, " +
                    "${NotesTable.COLUMN_PAGE}, ${NotesTable.COLUMN_IMAGE_NOTE}, ${NotesTable.COLUMN_DATE}) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf(username, imageUri.toString(), "${username}: \uD83D\uDCF7 - $note  ($selectedTrainer $selectedPage)", selectedTrainer, selectedPage, note, LocalDateTime.now().toString()))
        dbHelper.writableDatabase.close()

        updateNotesListView() // calls function to update notes list to display new note with image

        // returns the imageUri
        return imageUri
    }

    /**
     * Sets up and launches a dialog for a user to edit a note [selectedNote] and adjust any associations/flags with a note
     */
    private fun openEditNoteDialog(selectedNote: String) {
        // creates the dialog and finds the elements in the dialog
        val noteInteraction = DialogNoteInteractionBinding.inflate(LayoutInflater.from(this))
        val dialogBuilder = AlertDialog.Builder(this).setView(noteInteraction.root)
        val projection = arrayOf(NotesTable.COLUMN_DATE, NotesTable.COLUMN_IMAGE_URI, NotesTable.COLUMN_ISSUE, NotesTable.COLUMN_TRAINER, NotesTable.COLUMN_PAGE, NotesTable.NOTE_OWNER)
        val cursor = dbHelper.readableDatabase.query(NotesTable.TABLE_NAME, projection, "${NotesTable.COLUMN_NOTE} = ?", arrayOf(selectedNote),
            null, null, null).apply { moveToFirst() }

        // obtains the uri string based on the selected note
        imageUriString = cursor.getString(cursor.getColumnIndexOrThrow(NotesTable.COLUMN_IMAGE_URI)) ?: "DEFAULT"
        // obtains the status/state of the alert/issue switch based on selected note and set it to that state
        noteInteraction.customDialogAlertSwitch.isChecked = cursor.getInt(cursor.getColumnIndexOrThrow(NotesTable.COLUMN_ISSUE)) == 1

        // obtain and set the username tied to the note
        noteInteraction.usernameTextview.setText(cursor.getString(cursor.getColumnIndexOrThrow(NotesTable.NOTE_OWNER)).takeIf { it != " " }?: "NO OWNER FOUND")

        // obtain the date timestamp tied to the note
        noteInteraction.dateTextview.text = cursor.getString(cursor.getColumnIndexOrThrow(NotesTable.COLUMN_DATE)) ?: "NO DATE FOUND"

        // obtains the page number based on selected note and displays that
        noteInteraction.dropdownPages.setText(cursor.getString(cursor.getColumnIndexOrThrow(NotesTable.COLUMN_PAGE)).takeIf { it != "-" }?: "")

        // sets the image based on the obtained uri string
        noteInteraction.imageUriImageview.setImageURI(Uri.parse(imageUriString))

        // sets up the dropdown for the trainer
        setupDropdown(noteInteraction, cursor.getString(cursor.getColumnIndexOrThrow(NotesTable.COLUMN_TRAINER)))

        cursor.close()

        // set user action to display the image if it exists
        dialogBuilder.setNegativeButton("View Image") { _, _ ->
            runCatching {
                applicationContext.contentResolver.openInputStream(Uri.parse(imageUriString))?.use {
                    startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(imageUriString), "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }
            }.onFailure {
                Toast.makeText(this, "Image does not exist.", Toast.LENGTH_SHORT).show()
                imageUriString = ""
            }
        } // if image does not exist or if there is an issue, display a message indicating such

        if (!imageUriString.startsWith("content")) {
            dialogBuilder.setNegativeButton("Add Photo") { _, _ -> // if there is no image/uri, set user action to allow a photo to be taken
                setupAddPhotoButton(noteInteraction) // calls function to set up the button
                deletePhoto() // prevents a new note from being created and allows the current note to be edited
            }
        }

        // set user action to save the note, which calls the function to save note
        dialogBuilder.setPositiveButton("Save Note") { _, _ -> saveNote(noteInteraction, "Edit") }
        // set user action to delete the note, which calls the function to delete the note and any photos associated with it
        dialogBuilder.setNeutralButton("Delete") { _, _ -> deletePhoto() }

        // create the dialog box
        dialogBuilder.create().apply {
            noteInteraction.btnCancel.setOnClickListener { dismiss() } // set the action to close out dialog
            setOnShowListener { if (isDarkMode(context)) { darkModeBtn(context, this) } }
            setCancelable(false) // prevent user from disabling dialog by clicking outside

            val comment = commentForImage()
            noteInteraction.imageComment.setText(comment) // Retrieve the current comment associated with the image and set it as the text
            show() // show dialog

            // set the counter up in dialog
            noteInteraction.counterTextview.text = resources.getQuantityString(R.plurals.character_count, comment?.length ?: 0, comment?.length ?: 0, 150)
            createTextWatcher(noteInteraction)
        }
    }

    /**
     * Retrieves any comment associated with image
     */
    private fun commentForImage(): String? {
        val (selection, selectionArgs) = if (imageUriString.isBlank()) { "${NotesTable.COLUMN_IMAGE_URI} IS NULL OR ${NotesTable.COLUMN_IMAGE_URI} = ''" to emptyArray() } else {
            "${NotesTable.COLUMN_IMAGE_URI} = ?" to arrayOf(imageUriString) }
        return dbHelper.readableDatabase.use { db -> db.rawQuery("SELECT ${NotesTable.COLUMN_IMAGE_NOTE} FROM ${NotesTable.TABLE_NAME} WHERE $selection", selectionArgs).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(NotesTable.COLUMN_IMAGE_NOTE)) else null
            }
        }
    }

    /**
     * Deletes a photo when a user requests such action from a dialog
     */
    private fun deletePhoto() {
        dbHelper.writableDatabase.delete(NotesTable.TABLE_NAME, "${NotesTable.COLUMN_IMAGE_URI} = ?", arrayOf(imageUriString))
        if (!imageUriString.contains("default_image")) { applicationContext.contentResolver.delete(Uri.parse(imageUriString), null, null) }
        updateNotesListView()
    }

    /**
     * Saves a note based on user specified information into the database
     */
    private fun saveNote(noteInteraction: DialogNoteInteractionBinding, flag: String) {
        // obtain the note from the user
        val username = noteInteraction.usernameTextview.text.toString()
        // obtain the note
        val noteText = noteInteraction.imageComment.text.toString()
        // check if it is blank, if so throw message, else continue
        if (noteText.isBlank() || username.isBlank()) {
            Toast.makeText(this, "Note and/or Username is blank. Please input text.", Toast.LENGTH_SHORT).show()
        } else {
            // get the flags for if the note is private and/or an issue
            val isIssue = noteInteraction.customDialogAlertSwitch.isChecked
            // obtain the optional information such as trainer/doc and page number
            var selectedTrainer = noteInteraction.dropdownTrainer.selectedItem?.toString()
            var selectedPageNumber = noteInteraction.dropdownPages.text?.toString()
            if (selectedTrainer == null) {
                selectedTrainer = "Select Trainer"
            }
            if (selectedPageNumber == null) {
                selectedPageNumber = "0"
            }
            // format the information into a format for the app to read the note
            val noteContent = buildString {
                append("${username}: ")
                if (isIssue) append(" ❗")
                if (imageUriString.contains("content")) append(" \uD83D\uDCF7")
                append(" - $noteText  ($selectedTrainer $selectedPageNumber)")
            }
            // obtain the image uri if an image exists
            val newImageUriString = if (flag == "New") "default_image_${LocalDateTime.now()}.jpg" else imageUriString
            // save the data into the database
            val newValues = ContentValues().apply {
                put(NotesTable.NOTE_OWNER, username)
                put(NotesTable.COLUMN_DATE, LocalDateTime.now().toString())
                put(NotesTable.COLUMN_NOTE, noteContent)
                put(NotesTable.COLUMN_ISSUE, isIssue)
                put(NotesTable.COLUMN_TRAINER, selectedTrainer)
                put(NotesTable.COLUMN_PAGE, selectedPageNumber)
                put(NotesTable.COLUMN_IMAGE_URI, newImageUriString)
                put(NotesTable.COLUMN_IMAGE_NOTE, noteText)
            }

            dbHelper.readableDatabase.query(NotesTable.TABLE_NAME, null, "${NotesTable.COLUMN_IMAGE_URI} = ?", arrayOf(newImageUriString), null, null, null).use { cursor ->
                if (cursor.moveToFirst()) { dbHelper.writableDatabase.update(NotesTable.TABLE_NAME, newValues, "${NotesTable.COLUMN_IMAGE_URI} = ?", arrayOf(imageUriString)) }
                else { dbHelper.writableDatabase.insert(NotesTable.TABLE_NAME, null, newValues) }
            }
            this@NoteActivity.imageUriString = ""
            // update the display
            updateNotesListView()

            // if the note is an issue, send a message to teams to display
            if (isIssue) {
                TeamsAPI().send(Note("$username raised an issue:", noteText), selectedTrainer, selectedPageNumber)
            }

            activityNote.userSpinner.apply {
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { updateNotesListView() }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Create the list of options starting with the default option
                val usernameList = mutableListOf("All Notes")
                // Query the database to get the list of usernames
                DatabaseDbHelper(context).readableDatabase.query(true, NotesTable.TABLE_NAME, arrayOf(NotesTable.NOTE_OWNER),
                    null, null, null, null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) { cursor.getString(cursor.getColumnIndexOrThrow(NotesTable.NOTE_OWNER))?.takeIf { it.isNotBlank() }?.let { usernameList.add(it) } }
                }
                // Set the adapter for the spinner
                adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, usernameList) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent)
                        // Set text color to black for the spinner item
                        (view as TextView).setTextColor(-0x1000000)
                        return view
                    }
                }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
        }
    }


    /**
     * Creates the text watcher to which it updates and shows the character count of a user input
     */
    private fun createTextWatcher(noteInteraction: DialogNoteInteractionBinding) {
        noteInteraction.imageComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    noteInteraction.counterTextview.text = resources.getQuantityString(R.plurals.character_count, it.length, it.length, 150)
                    // get the current length of the edited text and set the text character count appropriately
                    if (it.length > 150) {
                        // prevent user from exceeding 150 character max
                        noteInteraction.imageComment.setText(it.subSequence(0, 150))
                        noteInteraction.imageComment.setSelection(it.subSequence(0, 150).length)
                    }
                }
            }
        })
    }

    /**
     * Updates the notes display with any new or edited information or updates the notes display with current information based
     * on what page the user is on for notes
     */
    private fun updateNotesListView() {
        val publicNotes = mutableListOf<String>()

        // obtain the data from database
        dbHelper.readableDatabase.query(NotesTable.TABLE_NAME, arrayOf(NotesTable.COLUMN_ID, NotesTable.COLUMN_NOTE),
            null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) { publicNotes.add(cursor.getString(cursor.getColumnIndexOrThrow(NotesTable.COLUMN_NOTE))) }
        }

        val selectedUsername = activityNote.userSpinner.selectedItem?.toString()
        val filteredNotes = if (selectedUsername == "All Notes") { publicNotes }
        else { publicNotes.filter { it.split(":").firstOrNull() == selectedUsername } }

        // obtain the current page and total pages that exist
        totalPages = maxOf((filteredNotes.size + 4) / 5, 1)
        currentPage = currentPage.coerceIn(1, totalPages).takeIf { filteredNotes.isNotEmpty() } ?: 1

        // set the note adapter to properly display notes
        activityNote.notesListview.adapter = NoteAdapter(this@NoteActivity, R.layout.list_item_note, filteredNotes.subList((currentPage - 1) * 5, minOf(currentPage * 5, filteredNotes.size)))
        // set the click action to open the edit dialog for the desired note
        activityNote.notesListview.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val note = filteredNotes.getOrNull((currentPage - 1) * 5 + position)
            note?.let { openEditNoteDialog(it) }
        }

        // set the page count display and navigation colors based on current page number
        activityNote.pageCountText.text = getString(R.string.page_count, currentPage, totalPages)
        activityNote.backArrow.setColorFilter(if (currentPage == 1 || totalPages == 1) -0x777778 else -0x1000000)
        activityNote.forwardArrow.setColorFilter(if (totalPages == 1 || currentPage == totalPages) -0x777778 else -0x1000000)
    }

    /**
     * Sets up the "Add Photo" buttons within the dialog to allow user to launch the camera
     * and save the note with associated information alongside the image that is to be taken
     */
    private fun setupAddPhotoButton(noteInteraction: DialogNoteInteractionBinding) {
        // checks if the note is blank, if so display message to enter a note before adding a photo
        if (noteInteraction.imageComment.text.toString().isBlank()) { Toast.makeText(this, "Please enter a note before adding photo", Toast.LENGTH_SHORT).show() }
        else { // checks the permission and launches settings instead of camera if permission is not granted
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePictureContract.launch(createImageUri(
                    noteInteraction.usernameTextview.text.toString(),
                    noteInteraction.imageComment.text.toString(),
                    noteInteraction.dropdownTrainer.selectedItem.toString(),
                    noteInteraction.dropdownPages.text.toString()
                )) }
            else { ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100) }
        }
    }

    /**
     * Develops the dropdowns for the pdf files and binds them to the note dialogs [noteInteraction]
     */
    private fun setupDropdown(noteInteraction: DialogNoteInteractionBinding, lastSelectedTrainer: String) {
        // obtain the directory of the pdf
        var pdfList: List<String> = listOf("Select Trainer")
        lifecycleScope.launch {
            try {
                // create a URL object with the server's address
                val url = URL("http://10.2.23.104:1025/list")

                // get the list of files from the server
                val files: List<String> = withContext(Dispatchers.IO) {
                    val connection = url.openConnection() as HttpURLConnection
                    connection.inputStream.bufferedReader().use { it.readText() }.let { json ->
                        // parse the JSON response into a list of strings
                        val type = object : TypeToken<List<String>>() {}.type
                        Gson().fromJson(json, type)
                    }
                }
                withContext(Dispatchers.Main) {
                    // filter the list of files to only include PDFs
                    pdfList = listOf("Select Trainer") + files.filter { it.endsWith(".pdf", ignoreCase = true) }
                }
                // Now you can use pdfList for your needs

            } catch (e: Exception) {
                // handle the exception
                Log.e("GetPDFsFromServer", "Error getting PDFs from server", e)
            }



        noteInteraction.dropdownTrainer.apply {
            // set the adapter to display the dropdown correctly in desired format
            adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, pdfList) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return super.getView(position, convertView, parent).apply { (this as TextView).setTextColor(-0x1000000) }
                }
            }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            // set the selection of dropdown to be the last selected trainer OR default value
            setSelection(pdfList.indexOf(lastSelectedTrainer))
            // set the click action to select the pdf/trainer and determine page numbers
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // get user selection
                    val selectedTrainer = parent?.getItemAtPosition(position).toString()
                    // read the pdf and obtain the page count
                    var maxPageNumber = 0
                    if (selectedTrainer != "Select Trainer") {
                        lifecycleScope.launch {
                            try {
                                // download the PDF file from the server
                                val pdfUrl = URL("http://10.2.23.104:1025/$selectedTrainer")
                                val pdfFile = File(context.getExternalFilesDir(null), selectedTrainer)
                                withContext(Dispatchers.IO) {
                                    pdfFile.outputStream().use { outputStream ->
                                        pdfUrl.openStream().use { it.copyTo(outputStream) }
                                    }
                                }

                                // get the page count
                                maxPageNumber = PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)).pageCount
                                // display the max amount of pages
                                noteInteraction.maxPageDisplay.text = buildString { append("/$maxPageNumber") }

                            } catch (e: Exception) {
                                // handle the exception
                                Log.e("GetPDFPageCount", "Error getting PDF page count", e)
                            }
                        }
                    }
                    noteInteraction.maxPageDisplay.text = buildString { append("/$maxPageNumber") }
                    noteInteraction.dropdownPages.apply {
                        // show a default value of 0 if its a default value, else prompt user with indicator to enter page number
                        hint = if (selectedTrainer != "Select Trainer") "Page #" else "0"
                        // enable the field for editing if not a default value
                        isEnabled = selectedTrainer != "Select Trainer"
                        // set the filter to be between 1 and the page count, if not a default value
                        filters = arrayOf(InputFilter { source, _, _, dest, dstart, dend ->
                            val newString = (dest.subSequence(0, dstart).toString() + source + dest.subSequence(dend, dest.length))
                            if (newString.isBlank() || newString.toIntOrNull() in 1..maxPageNumber) null else {
                                Toast.makeText(context, "Input must be between 1 and $maxPageNumber", Toast.LENGTH_SHORT).show()
                                ""
                            }
                        })
                    }

                }
                override fun onNothingSelected(parent: AdapterView<*>?) {} // does nothing but is needed
            }
        }
        }
    }

    override fun onDestroy() { // if notes activity is closed, destroy
        dbHelper.close() // close databases
        unregisterReceiver(closeNoteActivityReceiver) // remove intent receiver
        super.onDestroy()
    }
}