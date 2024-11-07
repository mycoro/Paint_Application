package edu.tcu.mlcoronilla.paint

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Bitmap
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
import com.bumptech.glide.Glide

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
        setUpSave()
        findViewById<ImageView>(R.id.undo_iv).setOnClickListener{
            drawingView.undoPath()
        }
    }

    //to set up the colors to be able to set up the change of color
    private fun setUpPalette(drawingView: DrawingView) {
        val paletteContainer = findViewById<LinearLayout>(R.id.palette_container)
       

        for (i in 0 until paletteContainer.childCount) {
            val colorView = paletteContainer.getChildAt(i) as ImageView

            //to get the color that has been clicked and get the color of background as path color
            colorView.setOnClickListener{
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
        val pickMedia = registerForActivityResult(PickVisualMedia()) {
            it?.let{Glide.with(this).load(it).into(backgroundIv)}
        }
        //use a listener to launch the sheet with the gallery icon
        findViewById<ImageView>(R.id.gallery_iv).setOnClickListener { pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }


    }

    private fun setUpSave() {
        findViewById<FrameLayout>(R.id.drawing_fl).post {
            val bitmap =
                findViewById<FrameLayout>(R.id.drawing_fl).drawToBitmap() //generates the image

            val values = ContentValues().apply { // to generate the uri
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    System.currentTimeMillis().toString() + ".jpeg"
                )
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let { contentResolver.openOutputStream(uri).use { image ->
                    image?.let { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, image) } //to store the image at URI
                }
            }
        }
    }

}