package com.example.screen4splds

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var nextButton: Button
    private var selectedLanguage: String = ""
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private lateinit var currentPhotoPath: String
    private lateinit var cameraResultLauncher: ActivityResultLauncher<Intent>

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show()
        }
    }

    private val capturePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            if (::photoUri.isInitialized) {
                cropImage(photoUri)
            } else {
                Toast.makeText(this, "Photo URI not initialized", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    private val selectPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            cropImage(it)
        }
    }

    private val uCropResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                showCategorySelectionDialog(it)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        radioGroup = findViewById(R.id.radioGroup)
        nextButton = findViewById(R.id.btn_next)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedLanguage = when (checkedId) {
                R.id.rb_languangeBengali -> "BE"
                R.id.rb_labguageSinhala -> "SI"
                R.id.rb_languageTamil -> "TA"
                else -> ""
            }
        }

        nextButton.setOnClickListener {
            if (selectedLanguage.isNotEmpty()) {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            } else {
                Toast.makeText(this, "Please select a language", Toast.LENGTH_SHORT).show()
            }
        }

        cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                if (::photoUri.isInitialized) {
                    cropImage(photoUri)
                } else {
                    Toast.makeText(this, "Photo URI not initialized", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile: File = File.createTempFile(imageFileName, ".jpg", storageDir)

        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            imageFile
        )
        currentPhotoPath = imageFile.absolutePath

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        cameraResultLauncher.launch(intent)
    }

    private fun cropImage(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "croppedImage.jpg"))
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)
        uCropResult.launch(uCrop.getIntent(this))
    }

    private fun showCategorySelectionDialog(uri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_category_selection, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupCategory)

        AlertDialog.Builder(this)
            .setTitle("Select Category")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val selectedId = radioGroup.checkedRadioButtonId
                val category = when (selectedId) {
                    R.id.radioButtonNormal -> "N"
                    R.id.radioButtonSpLD -> "S"
                    else -> "N"
                }
                saveCroppedImage(uri, category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCroppedImage(uri: Uri, category: String) {
        val timeStamp = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US).format(Date())
        val fileName = "${selectedLanguage}_${timeStamp}_$category.jpg"
        val storageDir: File? = getExternalFilesDir(null)
        val file = File(storageDir, fileName)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        val savedLocation = file.absolutePath
        Toast.makeText(this, "Image saved as $fileName\nLocation: $savedLocation\nSuccess!", Toast.LENGTH_LONG).show()
    }
}
