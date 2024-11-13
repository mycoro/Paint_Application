package edu.tcu.mlcoronilla.paint

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val drawingView = findViewById<DrawingView>(R.id.drawing_view) //the instance of kt file
        setUpPalette(drawingView)
        setUpPathWidthSelector(drawingView)
        val backgroundIv = findViewById<ImageView>(R.id.background_iv)
        setUpBackgroundPicker(backgroundIv)
        findViewById<ImageView>(R.id.save_iv).setOnClickListener {
            setUpSave(backgroundIv)
        }
        findViewById<ImageView>(R.id.undo_iv).setOnClickListener{
            drawingView.undoPath()
        }
    }

    //to set up the colors to be able to set up the change of color
    private fun setUpPalette(drawingView: DrawingView) {
        val paletteContainer = findViewById<LinearLayout>(R.id.palette_container)

        //sets the path_color_selected to black by default
        val initialColorView = paletteContainer.getChildAt(0) as ImageView
        initialColorView.setImageResource(R.drawable.path_color_selected) // Set default selected state

        var selectedColorView: ImageView? = initialColorView

        for (i in 0 until paletteContainer.childCount) {
            val colorView = paletteContainer.getChildAt(i) as ImageView

            //to set up whenever a color is clicked
            colorView.setOnClickListener {
                selectedColorView?.setImageResource(R.drawable.path_color_normal)

                colorView.setImageResource(R.drawable.path_color_selected)
                selectedColorView = colorView

                //to set color to drawing view
                val color = (colorView.background as ColorDrawable).color
                drawingView.setPathColor(color)
            }
        }
    }

    private fun setUpPathWidthSelector(drawingView: DrawingView) {

        findViewById<ImageView>(R.id.brush_icon).setOnClickListener{
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.path_width_selector)
            dialog.show()

            //to set listeners to image view widths and from there send width to drawingview file
            dialog.findViewById<ImageView>(R.id.first_width)?.setOnClickListener {
                drawingView.setPathWidth(5)
                dialog.dismiss()
            }
            dialog.findViewById<ImageView>(R.id.sec_width)?.setOnClickListener {
                drawingView.setPathWidth(10)
                dialog.dismiss()
            }
            dialog.findViewById<ImageView>(R.id.last_width)?.setOnClickListener {
                drawingView.setPathWidth(15)
                dialog.dismiss()
            }
        }
    }

    private fun setUpBackgroundPicker(backgroundIv: ImageView) {
        val pickMedia = registerForActivityResult(PickVisualMedia()) { //note the it is an URI
            it?.let{Glide.with(this).load(it).into(backgroundIv)}
        }
        //a listener for gallery imageview to launch the background picture
        findViewById<ImageView>(R.id.gallery_iv).setOnClickListener { pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }

    }

    private fun setUpSave(backgroundIv: ImageView) {
        val dialog = showInProgress()
        val drawingFrameLayout = findViewById<FrameLayout>(R.id.drawing_fl)

        lifecycleScope.launch(Dispatchers.IO) {
            val backgroundBitmap = if (backgroundIv.drawable != null) {
                backgroundIv.drawToBitmap()
            } else {
                null
            }

            val drawingBitmap = drawingFrameLayout.drawToBitmap()


            val finalBitmap = if (backgroundBitmap != null) {
                // if there is a background it merges with drawing
                Bitmap.createBitmap(backgroundBitmap.width, backgroundBitmap.height, Bitmap.Config.ARGB_8888).apply {
                    val canvas = Canvas(this)
                    canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
                    canvas.drawBitmap(drawingBitmap, 0f, 0f, null)
                }
            } else {
                //sets background image to white if nothing was drawn
                Bitmap.createBitmap(drawingFrameLayout.width, drawingFrameLayout.height, Bitmap.Config.ARGB_8888).apply {
                    val canvas = Canvas(this)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    canvas.drawBitmap(drawingBitmap, 0f, 0f, null)
                }
            }

            //saves the image
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis().toString().substring(2, 11) + ".jpeg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                contentResolver.openOutputStream(uri).use { stream ->
                    stream?.let { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                }
                uri
            }

            withContext(Dispatchers.Main) {
                dialog.dismiss()
                uri?.let { savedUri ->
                    //triggers the sharing pop up
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, savedUri)
                        type = "image/jpeg"
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Image"))
                }
            }
        }
    }

    private fun showInProgress():Dialog {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.in_progress)
        dialog.setCancelable(false)
        dialog.show()
        return dialog
    }
}