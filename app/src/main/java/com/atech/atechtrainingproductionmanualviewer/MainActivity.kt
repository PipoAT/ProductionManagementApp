package com.atech.atechtrainingproductionmanualviewer

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE
import androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL
import androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.atech.atechtrainingproductionmanualviewer.databinding.FragmentActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi

@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {

    // initialize the binding and the handler for auto logout functionality
    private lateinit var binding: FragmentActivityMainBinding
    private var isOpen = false

    object IDM {
        /**
         * Checks the device settings for if the device is in dark or light mode. If dark mode is enabled, it will return true, otherwise false
         */
        fun isDarkMode(context: Context): Boolean { return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES }

        /**
         * Sets the buttons of dialogs to be white to be visible if the device is in dark mode
         */
        fun darkModeBtn(context: Context, dialog: AlertDialog) {
            // obtain all possible dialog buttons
            listOf(BUTTON_NEGATIVE, BUTTON_NEUTRAL, BUTTON_POSITIVE).forEach {
                // set each button text to the color white
                dialog.getButton(it)?.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
        }
    }

    /**
     * Toggles the note activity. If [open] is true, then it closes, otherwise opposite action occurs.
     */
    private fun toggleNoteActivity(open: Boolean) {
        // closes the note activity if the flag is true, else opens it
        if (open) { this.sendBroadcast(Intent(NoteActivity.ACTION_CLOSE_NOTE_ACTIVITY)) }
        else { this.startActivity(Intent(this, NoteActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT }) }
        // change the state of the note navigation button to reflect the open/close state of the notes page
        isOpen = !isOpen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // keeps the application on at all times and not have device go to sleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // creates the layout
        binding = FragmentActivityMainBinding.inflate(layoutInflater)
        // set the view/screen with the binding.root
        setContentView(binding.root)

        // creates the navigation bar on the bottom of the app
        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment).navController
        setupActionBarWithNavController(navController, AppBarConfiguration(setOf(R.id.navigation_notes, R.id.navigation_dashboard)))
        binding.navView.setupWithNavController(navController)
        // sets the actions for each button click on the navbar
        binding.navView.apply {
            setOnItemSelectedListener { item ->
                when (item.itemId) { // open or close note activity on press
                    R.id.navigation_notes -> { toggleNoteActivity(isOpen)
                        true }
                    // navigate to the admin or dashboard page if needed
                    R.id.navigation_dashboard -> { navController.navigate(item.itemId)
                        true }
                    else -> false
                }
            }
        }
    }
}