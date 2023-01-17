package net.codebot.pdfviewer

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so we should expect people to need this.
// We may wish to provide this code.
class MainActivity : AppCompatActivity() {
    val LOGNAME = "PDFViewer"
    val FILENAME = "shannon1948.pdf"
    val FILERESID = R.raw.shannon1948

    // manage the pages of the PDF, see below
    lateinit var pdfRenderer: PdfRenderer
    lateinit var parcelFileDescriptor: ParcelFileDescriptor
    var currentPage: PdfRenderer.Page? = null

    // custom ImageView class that captures strokes and draws them over the image
    lateinit var pageImage: PDFimage
    lateinit var layout: LinearLayout
    val landscapeWidth: Int = 2560
    val landscapeHeight: Int = 1392
    val portraitWidth: Int = 1800
    val portraitHeight: Int = 2152

    // Store the page data into the saveData class
    var currPage: Int = 0

    // Record the current page number and total page number of pdf
    var totalPDFPage: Int = 0

    // Save the data of the app
    var savePageData: ArrayList<saveData> = ArrayList()

    // Record the orientation
    var appOrientation = "portrait"

    // Find the textview to record page number
    lateinit var pageNumber: TextView

    // Create shared preference for saving data
    lateinit var sp1: SharedPreferences
    lateinit var sp2: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        layout = findViewById(R.id.pdfLayout)
        pageNumber = findViewById(R.id.pageText)
        layout.isEnabled = true

        pageImage = PDFimage(this)
        layout.addView(pageImage)

        pageImage.minimumWidth = 1000
        pageImage.minimumHeight = 2000
        pageImage.maxWidth = 1000
        pageImage.maxHeight = 2000

        // Retrieve the saved data
        sp1 = getSharedPreferences(LOGNAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json1 = sp1.getString("${LOGNAME}1", "")
        if (json1!!.isNotEmpty()) {
            val listType: Type = object : TypeToken<ArrayList<saveData>>() {}.type
            savePageData = gson.fromJson(json1, listType)

            // Draw the paths saved inside savePageData
            for(savedDatas in savePageData) {
                if (savedDatas.pencilPaths.isNotEmpty()) {
                    for (path in savedDatas.pencilPaths) {
                        Log.d(LOGNAME, "Draw path: $path")
                        path!!.drawPath()
                    }
                }

                if (savedDatas.brushPaths.isNotEmpty()) {
                    for (path in savedDatas.brushPaths) {
                        path!!.drawPath()
                    }
                }

                if (savedDatas.undoStackPaths.isNotEmpty()) {
                    for (path in savedDatas.undoStackPaths) {
                        path!!.drawPath()
                    }
                    savedDatas.undoStack.paths = savedDatas.undoStackPaths
                }

                if (savedDatas.redoStackPaths.isNotEmpty()) {
                    for (path in savedDatas.redoStackPaths) {
                        path!!.drawPath()
                    }
                    savedDatas.redoStack.paths = savedDatas.redoStackPaths
                }

                if (savedDatas.undoStack.isNotEmpty()) {
                    savedDatas.undoStack.drawPaths()
                }

                if (savedDatas.redoStack.isNotEmpty()) {
                    savedDatas.redoStack.drawPaths()
                }
            }
        }

        sp2 = getSharedPreferences(LOGNAME, Context.MODE_PRIVATE)
        val json2 = sp2.getString("${LOGNAME}2", "")
        if (json2!!.isNotEmpty()) {
            val classType = pageClass(1)
            val savedCurrPage = gson.fromJson(json2, classType.javaClass)
            currPage = savedCurrPage.currPage
        }

        // If the user has not zoomed before, scale the image to fit the layout width
        if (savePageData.size > currPage) {
            if (savePageData[currPage].orientation == "landscape") {
                pageImage.scaleX = 3.0F
                pageImage.scaleY = 3.0F
                pageImage.translationX = 0.0F
                pageImage.translationY = 0.0F
            } else {
                pageImage.scaleX = 1.0F
                pageImage.scaleY = 1.0F
                pageImage.translationX = 0.0F
                pageImage.translationY = 0.0F
            }
        }

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this)
            totalPDFPage = pdfRenderer.pageCount
            showPage(currPage)
        } catch (exception: IOException) {
            Log.d(LOGNAME, "Error opening 1PDF")
        }

        // Add click listener to the feature buttons
        val pencilButton = findViewById<ImageButton>(R.id.pencilButton)
        pencilButton.setOnClickListener {
            pageImage.drawMethod = PDFimage.DRAW.PENCIL
            pageImage.imageMode = PDFimage.MODE.DRAW
            pageImage.eraser = false
        }

        val brushButton = findViewById<ImageButton>(R.id.highlighterButton)
        brushButton.setOnClickListener {
            pageImage.drawMethod = PDFimage.DRAW.BRUSH
            pageImage.imageMode = PDFimage.MODE.DRAW
            pageImage.eraser = false
        }

        val cursorButton = findViewById<ImageButton>(R.id.cursorButton)
        cursorButton.setOnClickListener {
            pageImage.drawMethod = PDFimage.DRAW.NONE
            pageImage.imageMode = PDFimage.MODE.NONE
            pageImage.eraser = false
        }

        val eraserButton = findViewById<ImageButton>(R.id.eraserButton)
        eraserButton.setOnClickListener{
            pageImage.drawMethod = PDFimage.DRAW.NONE
            pageImage.imageMode = PDFimage.MODE.ERASER
            pageImage.eraser = true
        }

        val nextButton = findViewById<Button>(R.id.nextButton)
        val prevButton = findViewById<Button>(R.id.prevButton)
        if (currPage == 0) {
            prevButton.isEnabled = false
        }
        nextButton.setOnClickListener {
            if (currPage < totalPDFPage - 1) {
                // Save the data of the previous page when move pages
                updateSavedData()

                currPage += 1
                if (!prevButton.isEnabled) {
                    prevButton.isEnabled = true
                }
            }

            if (currPage == totalPDFPage - 1) {
                nextButton.isEnabled = false
            }

            try {
                showPage(currPage)
            } catch (exception: IOException) {
                Log.d(LOGNAME, "Error opening PDF")
            }
        }

        prevButton.setOnClickListener {
            if (currPage > 0) {
                // Save the data of the previous page when move pages
                updateSavedData()

                currPage -= 1
                if (!nextButton.isEnabled) {
                    nextButton.isEnabled = true
                }
            }

            if (currPage == 0) {
                prevButton.isEnabled = false
            }

            try {
                showPage(currPage)
            } catch (exception: IOException) {
                Log.d(LOGNAME, "Error opening PDF")
            }
        }

        val undoButton = findViewById<ImageButton>(R.id.undoButton)
        undoButton.setOnClickListener {
            pageImage.undo()
        }

        val redoButton = findViewById<ImageButton>(R.id.redoButton)
        redoButton.setOnClickListener {
            pageImage.redo()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflacter = menuInflater
        inflacter.inflate(R.menu.mainmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.resetItem) {
            pageImage.translationX = 0.0F
            pageImage.translationY = 0.0F
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        Log.d(LOGNAME, "App resumes")
        super.onResume()
    }

    override fun onRestart() {
        Log.d(LOGNAME, "App restarts")
        super.onRestart()
        try {
            openRenderer(this)
            showPage(currPage)
        } catch (exception: IOException) {
            Log.d(LOGNAME, "Error opening 1PDF")
        }
    }

    override fun onStop() {
        // Save the data of the current page when close app
        updateSavedData()

        // Save the data from the app is closed
        val editor1 = sp1.edit()
        val gson = Gson()
        val json1 = gson.toJson(savePageData)
        editor1.putString("${LOGNAME}1", json1)
        editor1.commit()

        val editor2 = sp2.edit()
        val pageclass = pageClass(currPage)
        val json2 = gson.toJson(pageclass)
        editor2.putString("${LOGNAME}2", json2)
        editor2.commit()

        super.onStop()
    }

    override fun onDestroy() {
        // Save the data of the current page when close app
        updateSavedData()

        // Save the data from the app is closed
//        val editor = sp1.edit()
//        val gson = Gson()
//        val json = gson.toJson(savePageData)
//        editor.putString(LOGNAME, json)
//        editor.commit()

        super.onDestroy()
        try {
            closeRenderer()
        } catch (ex: IOException) {
            Log.d(LOGNAME, "Unable to close PDF renderer")
        }
    }

    @Throws(IOException::class)
    private fun openRenderer(context: Context) {
        // In this sample, we read a PDF from the assets directory.
        val file = File(context.cacheDir, FILENAME)
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            val asset = this.resources.openRawResource(FILERESID)
            val output = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var size: Int
            while (asset.read(buffer).also { size = it } != -1) {
                output.write(buffer, 0, size)
            }
            asset.close()
            output.close()
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        pdfRenderer = PdfRenderer(parcelFileDescriptor)
    }

    // do this before you quit!
    @Throws(IOException::class)
    private fun closeRenderer() {
        currentPage?.close()
        pdfRenderer.close()
        parcelFileDescriptor.close()
    }

    private fun showPage(index: Int) {
        if (pdfRenderer.pageCount <= index) {
            return
        }
        // Close the current page before opening another one.
        currentPage?.close()

        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index)

        // Retrieve the pre-stored data
        if (currPage < savePageData.size) {
            val currentData = savePageData[currPage]
            pageImage.pencilPaths = currentData.pencilPaths
            pageImage.brushPaths = currentData.brushPaths
            pageImage.undoStack = currentData.undoStack
            pageImage.redoStack = currentData.redoStack
        } else {
            pageImage.pencilPaths = ArrayList()
            pageImage.brushPaths = ArrayList()
            pageImage.undoStack = Memento()
            pageImage.redoStack = Memento()
        }

        val modifiedText = "Page ${currPage + 1}/${totalPDFPage}"
        pageNumber.text = modifiedText

        if (currentPage != null) {
            // Important: the destination bitmap must be ARGB (not RGB).
            val bitmap = Bitmap.createBitmap(currentPage!!.getWidth(), currentPage!!.getHeight(), Bitmap.Config.ARGB_8888)

            // Here, we render the page onto the Bitmap.
            // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
            // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
            currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            // Display the page
            pageImage.setImage(bitmap)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun rotatePathArray(paths: ArrayList<CustomPath?>) {
        val matrix = Matrix()
        matrix.reset()
        val scaleX = portraitWidth.toFloat() / landscapeWidth.toFloat()
        val translate = ((landscapeWidth - portraitWidth) / 2).toFloat()

        for (path in paths) {
            val startX = path!!.points[0].first
            matrix.reset()
            matrix.postTranslate(-startX, 0.0F)
            matrix.postScale(scaleX, 1.0F)
            path.transform(matrix)

            matrix.reset()
            val distX = landscapeWidth / 2 - (startX + translate)
            matrix.postTranslate(startX + distX * (1 - scaleX), 0.0F)
            matrix.postTranslate(translate, 0.0F)
            matrix.postScale(1.0F, scaleX)
            path.transform(matrix)
            path.updatePoints()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun rotatePathStack(paths: Stack<CustomPath>, savedData: saveData) {
        val matrix = Matrix()
        matrix.reset()
        val scaleY = portraitWidth.toFloat() / landscapeWidth.toFloat()
        val translate = ((landscapeWidth - portraitWidth) / 2).toFloat()

        for (path in paths) {
            if (savedData.pencilPaths.indexOf(path) == -1 && savedData.brushPaths.indexOf(path) == -1) {
                val startX = path!!.points[0].first
                matrix.reset()
                matrix.postTranslate(-startX, 0.0F)
                matrix.postScale(scaleY, 1.0F)
                path.transform(matrix)

                matrix.reset()
                val dist = landscapeWidth / 2 - (startX + translate)
                matrix.postTranslate(startX + dist * (1 - scaleY), 0.0F)
                matrix.postTranslate(translate, 0.0F)
                matrix.postScale(1.0F, scaleY)
                path.transform(matrix)
                path.updatePoints()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun rotateBackPathArray(paths: ArrayList<CustomPath?>) {
        val matrix = Matrix()
        matrix.reset()
        val scaleY = portraitWidth.toFloat() / landscapeWidth.toFloat()
        val translate = ((landscapeWidth - portraitWidth) / 2).toFloat()

        for (path in paths) {
            val startX = path!!.points[0].first
            matrix.reset()
            matrix.postTranslate(-startX, 0.0F)
            matrix.postScale((1.0F / scaleY), 1.0F)
            path.transform(matrix)

            matrix.reset()
            val dist = (landscapeWidth / 2 + (scaleY * translate - startX) / (1 - scaleY)) *
                    ((1 - scaleY) / (-scaleY))
            matrix.postTranslate(dist, 0.0F)
            matrix.postScale(1.0F, (1.0F / scaleY))
            path.transform(matrix)
            path.updatePoints()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun rotateBackPathStack(paths: Stack<CustomPath>, savedData: saveData) {
        val matrix = Matrix()
        matrix.reset()
        val scaleY = portraitWidth.toFloat() / landscapeWidth.toFloat()
        val translate = ((landscapeWidth - portraitWidth) / 2).toFloat()

        for (path in paths) {
            if (savedData.pencilPaths.indexOf(path) == -1 && savedData.brushPaths.indexOf(path) == -1) {
                val startX = path!!.points[0].first
                matrix.reset()
                matrix.postTranslate(-startX, 0.0F)
                matrix.postScale((1.0F / scaleY), 1.0F)
                path.transform(matrix)

                matrix.reset()
                val dist = (landscapeWidth / 2 + (scaleY * translate - startX) / (1 - scaleY)) *
                        ((1 - scaleY) / (-scaleY))
                matrix.postTranslate(dist, 0.0F)
                matrix.postScale(1.0F, (1.0F / scaleY))
                path.transform(matrix)
                path.updatePoints()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSavedData()
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            appOrientation = "landscape"
            // Transform the paths in the graph
            for (savedData in savePageData) {
                rotatePathArray(savedData.pencilPaths)
                rotatePathArray(savedData.brushPaths)
                rotatePathStack(savedData.undoStackPaths, savedData)
                rotatePathStack(savedData.redoStackPaths, savedData)
            }

//            updateSavedData()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            appOrientation = "portrait"
            for (savedData in savePageData) {
                rotateBackPathArray(savedData.pencilPaths)
                rotateBackPathArray(savedData.brushPaths)
                rotateBackPathStack(savedData.undoStackPaths, savedData)
                rotateBackPathStack(savedData.redoStackPaths, savedData)
            }

//            updateSavedData()
        }
        super.recreate()
    }

    // Save the data of the current page before changing the configuration
    fun updateSavedData() {
        if (savePageData.size == currPage) {
            savePageData.add(saveData(pageImage.pencilPaths, pageImage.brushPaths,
                pageImage.undoStack.paths, pageImage.redoStack.paths,
                pageImage.undoStack, pageImage.redoStack, appOrientation))
        } else {
            savePageData[currPage] = saveData(pageImage.pencilPaths, pageImage.brushPaths,
                pageImage.undoStack.paths, pageImage.redoStack.paths,
                pageImage.undoStack, pageImage.redoStack, appOrientation)
        }
    }

//    override fun onSaveInstanceState(outState: Bundle) {
//        with(outState) {
//            this.putSerializable("saveData", savePageData)
//        }
//        super.onSaveInstanceState(outState)
//    }
}