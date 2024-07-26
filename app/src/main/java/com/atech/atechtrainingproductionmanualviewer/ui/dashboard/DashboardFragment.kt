package com.atech.atechtrainingproductionmanualviewer.ui.dashboard

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.atech.atechtrainingproductionmanualviewer.R
import com.atech.atechtrainingproductionmanualviewer.databinding.CardLayoutBinding
import com.atech.atechtrainingproductionmanualviewer.databinding.FragmentDashboardBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class DashboardFragment : Fragment() {
    // initialize binding
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        lifecycleScope.launch {
            try {
                showErrorCard()
                // get the list of files from the server
                val url = URL("http://10.2.23.104:1025/list")
                val fileList: List<String> = withContext(Dispatchers.IO) {
                    val connection = url.openConnection() as HttpURLConnection
                    connection.inputStream.bufferedReader().use { it.readText() }.let { json ->
                        // parse the JSON response into a list of strings
                        val type = object : TypeToken<List<String>>() {}.type
                        Gson().fromJson(json, type)
                    }
                }

                val dropdownOptions = mutableListOf("All Manuals").apply { addAll(fileList.filter { it.endsWith(".pdf") }.map { it -> it.takeWhile { it != ' ' } }.distinct()) }

                // Create an ArrayAdapter with custom layout for dropdown items
                val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, dropdownOptions) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent)
                        // Set text color to black for the dropdown item
                        (view as TextView).setTextColor(-0x1000000)
                        return view
                    }
                }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

                // Set adapter to the dropdown
                withContext(Dispatchers.Main) {
                    binding.dropdown.adapter = adapter
                }

                // Call the function to update display to show only manuals with the prefix selected
                binding.dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        lifecycleScope.launch {
                            updateCardContainer(parent.getItemAtPosition(position).toString())
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            } catch (e: Exception) {
                // handle the exception
                Log.e("OnCreateView", "Error in onCreateView", e)
                showErrorCard()
            }

            // Add an onTextChangedListener to your searchBar, after a user enters text, it obtains and updates the display to only show cards containing user input
            binding.searchBar.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                // Call the function to update the display
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateCardContainer(s.toString()) }
            })

        }
        // Returns the layout with searchbar, dropdown, and main display
        return binding.root
    }

    private fun showErrorCard() {
        val cardBinding = CardLayoutBinding.inflate(LayoutInflater.from(requireContext()), binding.cardContainer, false)
        val numberOfViews = binding.cardContainer.childCount
        if (numberOfViews == 0) {
            TextView(requireContext()).apply {
                text = context.getString(R.string.error_no_documents_found)
                textSize = 20F
                setTextColor(-0x1)
                setPadding(75, 24, 0, 0)
                cardBinding.pdfNamesLayout.addView(this)
            }
            binding.cardContainer.addView(cardBinding.root)
        }
    }

    // when user leaves fragment, destroy it
    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        _binding = null
    }

    // when user returns to fragment, rebuild and resume usage
    override fun onResume() {
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        super.onResume()
    }

    /**
     * Updates the dashboard display by dynamically generating the cards that contain the trainer image and clickable
     * text that link to pdfs all based on the [prefix]
     */
    private fun updateCardContainer(prefix: String) = lifecycleScope.launch {
        try {
            // reset the view to be blank
            binding.cardContainer.removeAllViews()

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

            // filter the list of files to only include PDFs that match the prefix
            val pdfFiles = files.filter { it.endsWith(".pdf") && (prefix == "All Manuals" || it.contains(prefix, ignoreCase = true)) }
            // group the pdfs by their prefix
            pdfFiles.groupBy { it }
                .flatMap { (_, pdfsInGroup) -> pdfsInGroup.chunked(4) }
                .forEach { pdfChunk ->
                    val cardBinding = CardLayoutBinding.inflate(LayoutInflater.from(requireContext()), binding.cardContainer, false)
                    pdfChunk.forEach { pdfFile ->
                        // set the text to display pdf name/prefix with formatting
                        TextView(requireContext()).apply {
                            // set the text to the name of the pdf without the ".pdf"
                            text = pdfFile.substringBeforeLast('.')
                            // set the size of the text to font size 20
                            textSize = 20F
                            // add padding from the left and top to be within the right side of the blue container
                            setPadding(75, 24, 0, 0)
                            // set text color to white
                            setTextColor(-0x1)
                            // set the text to be clickable to open the pdf
                            setOnClickListener {
                                lifecycleScope.launch {
                                    Toast.makeText(requireContext(), "Loading PDF...", Toast.LENGTH_SHORT).show()
                                    val pdfUrl = URL("http://10.2.23.104:1025/$pdfFile")
                                    val pdfOutputFile = File(requireContext().getExternalFilesDir(null), pdfFile)
                                    withContext(Dispatchers.IO) {
                                        pdfOutputFile.outputStream().use { outputStream ->
                                            pdfUrl.openStream().use { it.copyTo(outputStream) }
                                        }
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", pdfOutputFile), "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    startActivity(intent)

                                }
                            }
                            // add the cards to the layout for user to see
                            cardBinding.pdfNamesLayout.addView(this)
                        }
                    }
                    // obtain the images from the same directory if available
                    // images are in the prefix.png naming format

                    try {
                        // create a URL object with the server's address
                        val imageUrl = URL("http://10.2.23.104:1025/${pdfChunk.first().takeWhile { it != ' ' }}.png")

                        // download the image from the server
                        val imageFile = File(requireContext().getExternalFilesDir(null), "$pdfChunk.png")
                        withContext(Dispatchers.IO) {
                            imageFile.outputStream().use { outputStream -> imageUrl.openStream().use { it.copyTo(outputStream) } }
                        }

                        // create a drawable from the downloaded image
                        val drawable = Drawable.createFromPath(imageFile.path)

                        // set the drawable to the ImageView
                        withContext(Dispatchers.Main) {
                            cardBinding.imageView.setImageDrawable(drawable ?: ContextCompat.getDrawable(requireContext(), R.drawable.default_image))
                        }

                    } catch (e: Exception) {
                        // handle the exception
                        Log.e("DownloadImage", "Error downloading image", e)
                        // if an error occurs, set the default image
                        withContext(Dispatchers.Main) {
                            cardBinding.imageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.default_image))
                        }
                    }
                    binding.cardContainer.addView(cardBinding.root)
                }
        } catch (e: Exception) {
            // handle the exception
            Log.e("UpdateCardContainer", "Error updating card container", e)
            showErrorCard()
        }
    }
}