package com.app.icyplay

import android.graphics.Color
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.content.ContentValues
import android.provider.BaseColumns
import android.util.Log
import com.google.gson.Gson
import android.content.Context
import android.content.Intent
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.FileProvider
import android.net.Uri
import android.preference.PreferenceManager
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.WindowInsetsController
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast

const val REQUEST_CODE_EXPORT = 1
const val REQUEST_CODE_IMPORT = 2


suspend fun downloadImageToInternalStorage(context: Context, imageUrl: String, fileName: String): String? {
    return withContext(Dispatchers.IO) { // Switch to background thread
        try {

            val url = URL(imageUrl)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()

            val inputStream: InputStream = connection.inputStream
            val fileOutputStream: FileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                fileOutputStream.write(buffer, 0, bytesRead)
            }

            fileOutputStream.close()
            inputStream.close()

            val file = File(context.filesDir, fileName)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            Log.d("ImageDownload", "Image saved at: ${file.absolutePath}")

            uri.toString() // Return the URI for use in WebView
        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }
}

data class GameInfo(
    val _id: String,
    val gameName: String,
    val userId: String,
    val password: String,
    val theme: String,
    val logoUrl: String,
    val tags: String,
    val isPinned: String,
    val modifiedAt: String,
    val remoteLogoUrl: String
)


class MainActivity : Activity() {


    private fun updateImagePathsInDatabase() {
        val dbHelper = IcyplayDatabaseHelper(this)
        val db = dbHelper.writableDatabase

        // Launch a coroutine on the Main thread
        CoroutineScope(Dispatchers.Main).launch {
            val cursor = db.query(
                IcyplayContract.GameEntry.TABLE_NAME,
                null, // null means all columns
                null,
                null,
                null,
                null,
                null
            )

            cursor.use {
                while (it.moveToNext()) {
                    val remoteLogoUrl = it.getString(
                        it.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_REMOTE_LOGO_URL)
                    )
                    val gameId = it.getLong(it.getColumnIndexOrThrow(BaseColumns._ID))

                    // Call the suspend function to download the image
                    val newLocalUri = withContext(Dispatchers.IO) {
                        downloadImageToInternalStorage(
                            this@MainActivity, // Use the Activity context explicitly
                            remoteLogoUrl,
                            "logo_$gameId.png"
                        )
                    }

                    // Update the logo_url column with the new local URI
                    newLocalUri?.let { uri ->
                        val values = ContentValues().apply {
                            put(IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL, uri)
                        }

                        withContext(Dispatchers.IO) {
                            db.update(
                                IcyplayContract.GameEntry.TABLE_NAME,
                                values,
                                "${BaseColumns._ID} = ?",
                                arrayOf(gameId.toString())
                            )
                        }
                    }
                }
            }

            db.close()
            Toast.makeText(this@MainActivity, "Image Paths Updated", Toast.LENGTH_SHORT).show()
            webView.evaluateJavascript("window.onDataImported();", null)
        }
    }


    private fun exportDatabaseToExternalStorage(uri: Uri) {
        val dbFile = getDatabasePath(IcyplayDatabaseHelper.DATABASE_NAME)  // Your SQLite database file
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            dbFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)  // Copy database to external storage
            }
            Toast.makeText(this, "Export Successful", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Export Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importDatabaseFromExternalStorage(uri: Uri) {
        val dbFile = getDatabasePath(IcyplayDatabaseHelper.DATABASE_NAME)  // Path to your local database

        // Open a connection to the database to query image paths
        val dbHelper = IcyplayDatabaseHelper(this)
        val db = dbHelper.readableDatabase

        // Query the database for all local image paths (content:// URIs)
        val cursor = db.query(
            IcyplayContract.GameEntry.TABLE_NAME,
            arrayOf(IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL),  // Only the column with image paths
            "${IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL} LIKE ?",  // Filter only local URIs
            arrayOf("content://%"),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val imageUri = it.getString(it.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL))
                try {
                    // Use ContentResolver to delete the file
                    val contentResolver = this@MainActivity.contentResolver
                    val deletedCount = contentResolver.delete(Uri.parse(imageUri), null, null)

                    if (deletedCount > 0) {
                        Log.d("DatabaseImport", "Successfully deleted image at: $imageUri")
                    } else {
                        Log.e("DatabaseImport", "Image not found or could not be deleted.")
                    }
                } catch (e: Exception) {
                    Log.e("DatabaseImport", "Error deleting image: ${e.message}")
                }
            }
        }

        db.close()  // Close the database connection

        // Proceed with overwriting the database
        contentResolver.openInputStream(uri)?.use { inputStream ->
            dbFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)  // Overwrite the existing database
                updateImagePathsInDatabase() // Call to update the image paths
            }
            Toast.makeText(this, "Import Successful", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Import Failed", Toast.LENGTH_SHORT).show()
        }
    }


    private lateinit var webView: WebView
    private lateinit var dbHelper: IcyplayDatabaseHelper

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        // Set the status bar color

        dbHelper = IcyplayDatabaseHelper(this)

        webView = findViewById(R.id.webview)
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
//        webView.webViewClient = WebViewClient()


        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.domStorageEnabled = true
        // Add JavaScript interface
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        // Fetch all game info
        // In onCreate method or where you are fetching game info
        webView.evaluateJavascript("getAllGameInfo()") { gameInfoJson ->
            // Parse the JSON response
            if (gameInfoJson == null || gameInfoJson == "null") {
                Log.e("MainActivity", "gameInfoJson is null or invalid")
                return@evaluateJavascript
            }
            val gameInfoList = Gson().fromJson(gameInfoJson, Array<GameInfo>::class.java)

            for (gameInfo in gameInfoList) {
                val logoUrl = gameInfo.logoUrl
                // Inject the image into the WebView using JavaScript
                webView.evaluateJavascript("javascript:addImageToWebView('$logoUrl')", null)
            }
        }

// Add a WebViewClient to handle page loading and interaction
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {

                super.onPageFinished(view, url)
                webView.visibility = View.VISIBLE
                // After the page has loaded, execute JavaScript to fetch and log game info
                webView.evaluateJavascript("fetchAndDisplayGames()", null)
            }
        }
        webView.loadUrl("file:///android_asset/index.html")
    }
    fun updateStatusBarTheme(isLightTheme: Boolean) {

        // For Android 11 and above (API level 30 and above)
        val controller = window.insetsController
        if (isLightTheme) {
            window.statusBarColor = Color.WHITE
            controller?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            // Use a custom color (e.g., #292929)
            window.statusBarColor = Color.parseColor("#17181e")
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = 0

        }

    }
    private var imagePickerCallbackUri: Uri? = null
    private fun getFileName(uri: Uri): String {
        var fileName = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = it.getString(displayNameIndex)
                }
            }
        }
        return fileName
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_EXPORT -> {
                    data?.data?.let { uri ->
                        exportDatabaseToExternalStorage(uri)
                        webView.evaluateJavascript("window.onDataExported();", null)  // Notify WebView
                    }
                }
                REQUEST_CODE_IMPORT -> {
                    data?.data?.let { uri ->
                        importDatabaseFromExternalStorage(uri)
                        webView.evaluateJavascript("window.onDataImported();", null)  // Notify WebView
                    }
                }
            }
        }

        if (requestCode == IMAGE_PICK_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                imagePickerCallbackUri = uri
                // Convert to base64 and send to WebView
                val fileName = getFileName(uri)
                val base64 = uriToBase64(uri)
                webView.evaluateJavascript("window.handleImagePick('$base64', '$fileName')", null)
            }
        }
    }
    private fun uriToBase64(uri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: byteArrayOf()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        private const val IMAGE_PICK_REQUEST = 1001
    }
    inner class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun openImagePicker() {
            (context as Activity).runOnUiThread {
                // Launch image picker
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                context.startActivityForResult(intent, IMAGE_PICK_REQUEST)
            }
        }
        @JavascriptInterface
        fun exportData() {
            // Launch the file picker for saving (EXPORT)
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"  // File type for SQLite database
                putExtra(Intent.EXTRA_TITLE, "GameInfo.db")  // Suggest a filename
            }
            startActivityForResult(intent, REQUEST_CODE_EXPORT)
        }

        @JavascriptInterface
        fun importData() {
            // Launch the file picker for selecting a file (IMPORT)
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"  // File type for SQLite database
            }
            startActivityForResult(intent, REQUEST_CODE_IMPORT)
        }
        @JavascriptInterface
        fun setTheme(theme: String) {
            this@MainActivity.runOnUiThread {
                val isLightTheme = theme == "light"
                updateStatusBarTheme(isLightTheme)
            }
        }
        @JavascriptInterface
        fun refresh(){
            this@MainActivity.runOnUiThread {
                webView.reload() // Reload the WebView
            }
        }
        @JavascriptInterface
        fun saveGameInfo(gameName: String, userId: String, password: String, theme: String, logoUrl: String, tags: String, isPinned : Boolean, modifiedAt: String,localLogoPicked: Boolean,base64Image: String) {
            CoroutineScope(Dispatchers.IO).launch {
            // Download the image to internal storage
                var storedLogoPath: String? = null

                if(localLogoPicked){
                    val pureBase64Image = base64Image.substringAfter(",")
                    val imageBytes = Base64.decode(pureBase64Image,Base64.NO_WRAP)
                    val file = File(context.filesDir, "logo_${System.currentTimeMillis()}.jpg")
                    file.writeBytes(imageBytes)

                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    storedLogoPath = uri.toString()

                }else if(logoUrl.isNotBlank()){
                    storedLogoPath   = downloadImageToInternalStorage(
                        this@MainActivity, logoUrl, "logo_${System.currentTimeMillis()}.jpg"
                    )
                }
            // If the image was successfully downloaded, use the stored path instead of the URL
            val logoPath = storedLogoPath ?: logoUrl
println(logoPath)
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(IcyplayContract.GameEntry.COLUMN_NAME_GAME_NAME, gameName)
                put(IcyplayContract.GameEntry.COLUMN_NAME_USER_ID, userId)
                put(IcyplayContract.GameEntry.COLUMN_NAME_PASSWORD, password)
                put(IcyplayContract.GameEntry.COLUMN_NAME_THEME, theme)
                put(IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL, logoPath)
                put(IcyplayContract.GameEntry.COLUMN_NAME_TAGS, tags)
                put(IcyplayContract.GameEntry.COLUMN_NAME_IS_PINNED, isPinned)
                put(IcyplayContract.GameEntry.COLUMN_NAME_MODIFIED_AT, modifiedAt)
                put(IcyplayContract.GameEntry.COLUMN_NAME_REMOTE_LOGO_URL, logoUrl)
            }

                // Perform the save operation in the background thread
                db.insert(IcyplayContract.GameEntry.TABLE_NAME, null, values)
                // After saving, switch to the main thread to notify JS
                withContext(Dispatchers.Main) {
                    // Notify the JS side that the save operation is done
                    webView.post {
                        webView.evaluateJavascript("window.onGameInfoSaved();", null)
                    }
                }
            }

        }
        @JavascriptInterface
        fun getAllGameInfo(): String {
            val db = dbHelper.readableDatabase
            val projection = arrayOf(
                BaseColumns._ID,
                IcyplayContract.GameEntry.COLUMN_NAME_GAME_NAME,
                IcyplayContract.GameEntry.COLUMN_NAME_USER_ID,
                IcyplayContract.GameEntry.COLUMN_NAME_PASSWORD,
                IcyplayContract.GameEntry.COLUMN_NAME_THEME,
                IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL,
                IcyplayContract.GameEntry.COLUMN_NAME_TAGS,
                IcyplayContract.GameEntry.COLUMN_NAME_IS_PINNED,
                IcyplayContract.GameEntry.COLUMN_NAME_MODIFIED_AT,
                IcyplayContract.GameEntry.COLUMN_NAME_REMOTE_LOGO_URL
            )

            val cursor = db.query(
                IcyplayContract.GameEntry.TABLE_NAME,
                projection, null, null, null, null, null
            )

            val gameInfoList = mutableListOf<HashMap<String, String>>()

            while (cursor.moveToNext()) {
                val gameInfo = HashMap<String, String>()
                gameInfo["_id"] = cursor.getString(cursor.getColumnIndexOrThrow(BaseColumns._ID))
                gameInfo["gameName"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_GAME_NAME))
                gameInfo["userId"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_USER_ID))
                gameInfo["password"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_PASSWORD))
                gameInfo["theme"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_THEME))
                gameInfo["logoUrl"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL))
                gameInfo["tags"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_TAGS))
                gameInfo["isPinned"] = (cursor.getInt(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_IS_PINNED)) == 1).toString()
                gameInfo["modifiedAt"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_MODIFIED_AT))
                gameInfo["remoteLogoUrl"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_REMOTE_LOGO_URL))
                gameInfoList.add(gameInfo)
            }
            cursor.close()

            // Sort the gameInfoList by isPinned and modifiedAt
            gameInfoList.sortWith(compareByDescending<HashMap<String, String>> { it["isPinned"] == "true" }
                .thenByDescending { it["modifiedAt"] }) // Assuming modifiedAt is in ISO format

            // Convert the list of game info into a JSON string
            val gson = Gson()
            val sortedGameInfoJson = gson.toJson(gameInfoList)
            Log.d("WebAppInterface", sortedGameInfoJson)
            return sortedGameInfoJson

        }
        @JavascriptInterface
        fun getGameInfoById(gameId: String): String {
            val db = dbHelper.readableDatabase
            val projection = arrayOf(
                BaseColumns._ID,
                IcyplayContract.GameEntry.COLUMN_NAME_GAME_NAME,
                IcyplayContract.GameEntry.COLUMN_NAME_USER_ID,
                IcyplayContract.GameEntry.COLUMN_NAME_PASSWORD,
                IcyplayContract.GameEntry.COLUMN_NAME_THEME,
                IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL,
                IcyplayContract.GameEntry.COLUMN_NAME_TAGS,
                IcyplayContract.GameEntry.COLUMN_NAME_IS_PINNED,
                IcyplayContract.GameEntry.COLUMN_NAME_MODIFIED_AT,
                IcyplayContract.GameEntry.COLUMN_NAME_REMOTE_LOGO_URL
            )

            // Define selection criteria for the query
            val selection = "${BaseColumns._ID} = ?"
            val selectionArgs = arrayOf(gameId)

            val cursor = db.query(
                IcyplayContract.GameEntry.TABLE_NAME,
                projection, selection, selectionArgs, null, null, null
            )

            // Check if a result was found
            if (cursor.moveToFirst()) {
                val gameInfo = HashMap<String, String>()
                gameInfo["_id"] = cursor.getString(cursor.getColumnIndexOrThrow(BaseColumns._ID))
                gameInfo["gameName"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_GAME_NAME))
                gameInfo["userId"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_USER_ID))
                gameInfo["password"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_PASSWORD))
                gameInfo["theme"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_THEME))
                gameInfo["logoUrl"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL))
                gameInfo["tags"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_TAGS))
                gameInfo["isPinned"] = (cursor.getInt(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_IS_PINNED)) == 1).toString()
                gameInfo["modifiedAt"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_MODIFIED_AT))
                gameInfo["remoteLogoUrl"] = cursor.getString(cursor.getColumnIndexOrThrow(IcyplayContract.GameEntry.COLUMN_NAME_REMOTE_LOGO_URL))
                cursor.close()

                // Convert the game info to a JSON string and log it
                val gson = Gson()
                val gameInfoJson = gson.toJson(gameInfo)
                Log.d("WebAppInterface", gameInfoJson)
                return gameInfoJson
            } else {
                cursor.close()
                Log.d("WebAppInterface", "No game found with ID: $gameId")
                return "{}" // Return an empty JSON object if no game is found
            }
        }

        @JavascriptInterface
        fun updateGameInfoById(
            gameId: String,
            gameName: String,
            userId: String,
            password: String,
            theme: String,
            logoUrl: String,
            tags: String,
            isPinned: Boolean,
            modifiedAt: String,
            localLogoPicked: Boolean,
            base64Image: String
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                // Perform the save operation in the background thread

                val db = dbHelper.writableDatabase

                // Get the current game info by ID to compare the logoUrl
                val currentGameInfoJson = getGameInfoById(gameId)
                val gson = Gson()
                val currentGameInfo = gson.fromJson(currentGameInfoJson, HashMap::class.java)

                // Check if the logoUrl has changed
                val currentRemoteLogoUrl = currentGameInfo["remoteLogoUrl"] as? String
                val oldCurrentUrl = currentGameInfo["logoUrl"] as? String
                var finalLogoUrl = logoUrl
                val contentResolver = this@MainActivity.contentResolver

                if(logoUrl.contains("http://") || logoUrl.contains("https://")) {

                    if (currentRemoteLogoUrl != logoUrl) {

                        var storedLogoPath: String? = null
                        if(logoUrl.isNotBlank()){
                            storedLogoPath   = downloadImageToInternalStorage(
                                this@MainActivity, logoUrl, "logo_${System.currentTimeMillis()}.jpg"
                            )
                        }
                        println("triggered $finalLogoUrl" )
                        // Check if the image was successfully downloaded
                        if (storedLogoPath != null) {

                            finalLogoUrl = storedLogoPath

                            try {
                                // Use ContentResolver to delete the file

                                contentResolver.delete(Uri.parse(oldCurrentUrl), null, null)

                            } catch (e: Exception) {
                                Log.e("WebAppInterface", "Error deleting image: ${e.message}")
                            }
                        }

                    }else{
                        if (oldCurrentUrl != null) {
                            finalLogoUrl = oldCurrentUrl
                        }
                    }
                }else{
                    if (oldCurrentUrl != logoUrl) {
                        var storedLogoPath: String? = null

                        if(localLogoPicked){
                            val pureBase64Image = base64Image.substringAfter(",")
                            val imageBytes = Base64.decode(pureBase64Image,Base64.NO_WRAP)
                            val file = File(context.filesDir, "logo_${System.currentTimeMillis()}.jpg")
                            file.writeBytes(imageBytes)

                            storedLogoPath = file.absolutePath

                        }else if(logoUrl.isNotBlank()){
                            storedLogoPath   = downloadImageToInternalStorage(
                                this@MainActivity, logoUrl, "logo_${System.currentTimeMillis()}.jpg"
                            )
                        }

                        // Check if the image was successfully downloaded
                        if (storedLogoPath != null) {

                            finalLogoUrl = storedLogoPath

                            try {
                                // Use ContentResolver to delete the file
                               contentResolver.delete(Uri.parse(oldCurrentUrl), null, null)
                            } catch (e: Exception) {
                                Log.e("WebAppInterface", "Error deleting image: ${e.message}")
                            }
                        }

                    }
                }


                // Prepare the new values to be updated
                val values = ContentValues().apply {
                    put(IcyplayContract.GameEntry.COLUMN_NAME_GAME_NAME, gameName)
                    put(IcyplayContract.GameEntry.COLUMN_NAME_USER_ID, userId)
                    put(IcyplayContract.GameEntry.COLUMN_NAME_PASSWORD, password)
                    put(IcyplayContract.GameEntry.COLUMN_NAME_THEME, theme)
                    put(IcyplayContract.GameEntry.COLUMN_NAME_LOGO_URL, finalLogoUrl)
                    put(IcyplayContract.GameEntry.COLUMN_NAME_TAGS, tags)
                    put(IcyplayContract.GameEntry.COLUMN_NAME_IS_PINNED, if (isPinned) 1 else 0)
                    put(IcyplayContract.GameEntry.COLUMN_NAME_MODIFIED_AT, modifiedAt)
                    put(IcyplayContract.GameEntry.COLUMN_NAME_REMOTE_LOGO_URL, logoUrl)
                }

                // Specify which row to update, based on the game ID
                val selection = "${BaseColumns._ID} = ?"
                val selectionArgs = arrayOf(gameId)

                // Perform the update and get the number of rows affected
                 db.update(
                    IcyplayContract.GameEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs
                )

                // After updating, switch to the main thread to notify JS
                withContext(Dispatchers.Main) {
                    // Notify the JS side with the result of the operation (success or failure)
                    webView.post {

                        webView.evaluateJavascript("window.onGameInfoUpdated();", null)
                    }
                }
            }
        }


        @JavascriptInterface
        fun updatePinnedStatusById(gameId: String, isPinned: Boolean, modifiedAt: String): Boolean {
            val db = dbHelper.writableDatabase

            // Prepare the new values to be updated (only isPinned and modifiedAt)
            val values = ContentValues().apply {
                put(IcyplayContract.GameEntry.COLUMN_NAME_IS_PINNED, if (isPinned) 1 else 0)
                put(IcyplayContract.GameEntry.COLUMN_NAME_MODIFIED_AT, modifiedAt)
            }

            // Specify which row to update, based on the game ID
            val selection = "${BaseColumns._ID} = ?"
            val selectionArgs = arrayOf(gameId)

            // Perform the update and check the number of rows affected
            val count = db.update(
                IcyplayContract.GameEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )

            // Return true if the update was successful, false otherwise
            return count > 0
        }


        @JavascriptInterface
        fun deleteGame(id: String, imageUri: String) {
            // Delete the database entry
            val rowsDeleted = dbHelper.deleteGameById(id)

            if (rowsDeleted > 0) {
                // Attempt to delete the image file
                try {
                    // Use ContentResolver to delete the file
                    val contentResolver = this@MainActivity.contentResolver
                    val deletedCount = contentResolver.delete(Uri.parse(imageUri), null, null)

                    if (deletedCount > 0) {
                        Log.d("WebAppInterface", "Successfully deleted image at: $imageUri")
                    } else {
                        Log.e("WebAppInterface", "Image not found or could not be deleted.")
                    }
                } catch (e: Exception) {
                    Log.e("WebAppInterface", "Error deleting image: ${e.message}")
                }
            }
            Log.d("WebAppInterface", "Deleted $rowsDeleted rows with ID: $id")
        }



    }
}


