package com.t.cameragallerykotlin

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var userChoosenTask: String? = null
    private var imageFile: File? = null
    var fileUri: Uri? = null

    companion object {
        val CONTENT_URI = "content://com.example"
        val PERMISSION_STORAGE = 101
        val REQUEST_GALLERY = 102
        val FILE_URI = "fileUri"
        val REQUEST_CAMERA = 101
        val PERMISSION_CAMERA = 102

        @Throws(IOException::class)
        fun copyInputStream(inputStream: InputStream, outputStream: OutputStream) {
            inputStream.use { input ->
                outputStream.use { fileOut ->
                    input.copyTo(fileOut)
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnUploadPic.setOnClickListener { selectImage() }
    }
    private fun selectImage() {
        val items = arrayOf<CharSequence>("Take Photo", "Choose from Library", "Cancel")

        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Add Photo!")
        builder.setItems(items) { dialog, item ->
            // boolean result= Utility.checkPermission(SignupAsCharityActivity.this);

            if (items[item] == "Take Photo") {
                userChoosenTask = "Take Photo"
                /*if(result)
                        cameraIntent();
*/
                isCameraPermissionGranted()
            } else if (items[item] == "Choose from Library") {
                userChoosenTask = "Choose from Library"
                //if(result)
                isReadWritePermissionGranted()
            } else if (items[item] == "Cancel") {
                dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun isCameraPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                takePicture()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_CAMERA
                )
            }
        } else {
            takePicture()
        }
    }

    // open camera intent
    private fun takePicture() {
        val state = Environment.getExternalStorageState()
        imageFile =
            File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), System.currentTimeMillis().toString() + ".jpeg")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (Environment.MEDIA_MOUNTED == state) {
                    fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", imageFile!!)
                } else {
                    fileUri = Uri.parse(CONTENT_URI)
                }
            } else {
                if (Environment.MEDIA_MOUNTED == state) {
                    fileUri = Uri.fromFile(imageFile)
                } else {
                    fileUri = Uri.parse(CONTENT_URI)
                }
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            intent.putExtra("return-data", true)
            intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            startActivityForResult(intent, REQUEST_CAMERA)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    // check read and write permission
    private fun isReadWritePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                chooseFromGallery()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), PERMISSION_STORAGE
                )
            }
        } else {
            chooseFromGallery()
        }
    }
    private fun chooseFromGallery() {
        val state = Environment.getExternalStorageState()
        imageFile =
            File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), System.currentTimeMillis().toString() + ".jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", imageFile!!)
        } else {
            if (Environment.MEDIA_MOUNTED == state) {
                fileUri = Uri.fromFile(imageFile)
            } else {
                fileUri = Uri.parse(CONTENT_URI)
            }
        }
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_GALLERY) {
                if (data != null) {
                    val selectedImage = data.data
                    if (selectedImage != null) {
                        val inputStream: InputStream?
                        try {
                            inputStream = contentResolver.openInputStream(selectedImage)
                            if (!imageFile!!.exists()) {
                                File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()).mkdir()
                            }
                            val fileOutputStream = FileOutputStream(imageFile)
                            if (inputStream != null) {
                                copyInputStream(inputStream, fileOutputStream)
                                inputStream.close()
                            }
                            fileOutputStream.close()
                            if (imageFile != null) {
                                applyExifInterface(imageFile!!.absolutePath)
                            }
                            /*var requestOptions = RequestOptions()
                            requestOptions = requestOptions.transforms(CenterCrop(), RoundedCorners(32))*/
                            Glide.with(this)
                                .load(imageFile!!.absolutePath)
                                .into(imgProblmereport!!)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }
                }
            }
            if (requestCode == REQUEST_CAMERA) {

                applyExifInterface(imageFile!!.absolutePath)
                /*var requestOptions = RequestOptions()
                requestOptions = requestOptions.transforms(CenterCrop(), RoundedCorners(32))
                */
                if (imageFile!!.exists()) {
                    imgProblmereport.visibility = View.VISIBLE
                    var image = BitmapFactory.decodeFile(imageFile!!.absolutePath)
                    var imageResized = resizeBitmap(image, 500,200)
                    imageFile=bitmapToFile(imageResized!!,this)
                    var imgttt=bitmapToFile(imageResized,this)

                    Glide.with(this)
                        .load(imgttt.absolutePath)
                        .into(imgProblmereport!!)
                }

            }
        }
        if (resultCode==101)
        {


        }

    }
    //    After clicked picture from camera or choose from gallery, apply below method in onActivityResult
    private fun applyExifInterface(path: String) {
        val exif: ExifInterface
        try {
            exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            var angle = 0
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> angle = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> angle = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> angle = 270
            }
            if (angle != 0) {
                val bmOptions = BitmapFactory.Options()
                var bitmap = BitmapFactory.decodeFile(path, bmOptions)
                val matrix = Matrix()
                matrix.postRotate(angle.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                val out = FileOutputStream(path)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()
                bitmap.recycle()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun bitmapToFile(bitmap: Bitmap,context: Context): File {
        // Get the context wrapper
        val wrapper = ContextWrapper(context)

        // Initialize a new file instance to save bitmap object
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")

        try{
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e: IOException){
            e.printStackTrace()
        }

        // Return the saved bitmap uri
        // return Uri.parse(file.absolutePath)
        return file
    }
    fun resizeBitmap(getBitmap: Bitmap?, maxSize: Int,height1:Int): Bitmap? {
        var width = getBitmap!!.getWidth()
        var height = getBitmap.getHeight()
        var x:Double

        if (width >= height && width > maxSize) {
            x = (width / height).toDouble()
            width = maxSize;
            height = ((height1 / x).toInt())
        } else if (height >= width && height > maxSize) {
            x = (height / width).toDouble()
            height = height1;
            width = ((maxSize / x).toInt())
        }
        return Bitmap.createScaledBitmap(getBitmap, width, height, false)
    }

    fun customDialog(context: Context, message: String) {
        val dialog = Dialog(context)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.prompts_post)

        /*val btnOk = dialog.findViewById<View>(R.id.btnOk) as Button
        btnOk.visibility = View.VISIBLE
        btnOk.text = context.resources.getString(R.string.ok)
        val tv_message = dialog.findViewById<View>(R.id.tv_message) as TextView
        //TextView tv_title=(TextView)dialog.findViewById(R.id.tv_title);
        //tv_title.setVisibility(View.GONE);
        // tv_title.setText(getString(R.string.congratulations));
        //tv_title.setGravity(Gravity.CENTER);
        //        Spannable sb = new SpannableString( message );
        //        sb.setSpan(new StyleSpan(Typeface.BOLD), message.indexOf("(") , message.indexOf("(")+14, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); //bold
        tv_message.text = message
        tv_message.gravity = Gravity.CENTER
        btnOk.setOnClickListener { dialog.dismiss() }*/
        dialog.show()
    }

}


