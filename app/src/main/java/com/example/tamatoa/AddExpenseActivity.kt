package com.example.tamatoa

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.tamatoa.data.AppDatabase
import com.example.tamatoa.data.CategoryEntity
import com.example.tamatoa.data.DateUtils
import com.example.tamatoa.data.ExpenseEntity
import com.example.tamatoa.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

//Form for creating a new expense
//Asks for amount,category, note, date, and if its recurring
//additionally asks for photo and allows you to create categories
//Information found at: https://developer.android.com/training/data-storage/room
//https://developer.android.com/training/data-storage/room/prepopulate
//https://developer.android.com/develop/ui/views/components/pickers
//for photo stuff specifically: https://www.youtube.com/watch?v=cZ323b1w-rc
//https://www.youtube.com/watch?v=FTKWgcIYomY
//for add category dialogs: https://www.youtube.com/watch?v=sNyZCyisxX0
//https://www.youtube.com/watch?v=TAWkdAafK4I
class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var selectedCategory: CategoryEntity? = null
    private var availableCategories: List<CategoryEntity> = emptyList()
    private var pendingPhotoFile: File? = null
    private var savedPhotoPath: String? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoFile != null) {
            savedPhotoPath = pendingPhotoFile!!.absolutePath
            Log.d(TAG, "take picture: saved at $savedPhotoPath")
            showPhotoPreview(pendingPhotoFile!!)
        } else {
            Log.d(TAG, "take picture: cancelled or failed")
            // delete the picture they took if they cancel or something goes wrong
            pendingPhotoFile?.delete()
            pendingPhotoFile = null
        }
    }

    companion object {
        private const val TAG = "AddExpenseActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // if user gets here without logging in, send them back
        if (!Session.isLoggedIn(this)) {
            Log.w(TAG, "on create: not logged in")
            finish()
            return
        }

        binding.closeButton.setOnClickListener { finish() }
        binding.dateRow.setOnClickListener { showDatePicker() }
        binding.categoryRow.setOnClickListener { showCategoryPicker() }
        binding.saveButton.setOnClickListener { attemptSave() }
        binding.photoFrame.setOnClickListener { launchCamera() }
        binding.photoRemoveButton.setOnClickListener { clearPhoto() }

        updateDateLabel()
        loadCategories()
    }

    //fetch the user added categories to load with the pre added ones
    private fun loadCategories() {
        val userId = Session.getUserId(this)
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@AddExpenseActivity)
                availableCategories = db.categoryDao().getForUser(userId)
                Log.d(TAG, "load categories: ${availableCategories.size} loaded")
            } catch (e: Exception) {
                Log.e(TAG, "load categories: error", e)
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(year, month, day)
                selectedDateMillis = cal.timeInMillis
                updateDateLabel()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateLabel() {
        binding.dateValue.text = DateUtils.formatShort(selectedDateMillis)
    }

    private fun showCategoryPicker() {
        // add all categories tied to the user and put the add category option at the top
        val items = mutableListOf("+ Add new category")
        items.addAll(availableCategories.map { it.name })

        AlertDialog.Builder(this)
            .setTitle("Choose a category")
            .setItems(items.toTypedArray()) { _, which ->
                if (which == 0) {
                    showNewCategoryDialog()
                } else {
                    selectedCategory = availableCategories[which - 1]
                    updateCategoryLabel()
                }
            }
            .show()
    }


    private fun showNewCategoryDialog() {
        val view = layoutInflater.inflate(R.layout.new_category, null)
        val nameInput = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.nameInput)

        //make each palette square the right colour
        val palettes = listOf(
            view.findViewById<android.view.View>(R.id.palettePink)   to "#D14B8F",
            view.findViewById<android.view.View>(R.id.paletteBlue)   to "#4B8FE0",
            view.findViewById<android.view.View>(R.id.paletteYellow) to "#E0C04B",
            view.findViewById<android.view.View>(R.id.paletteGreen)  to "#3DBE6F",
            view.findViewById<android.view.View>(R.id.paletteOrange) to "#F2A03D",
            view.findViewById<android.view.View>(R.id.palettePurple) to "#9B59B6"
        )
        var chosenColour = palettes[0].second   // default to the first colour option

        //draw a cute little border around the selected palette
        fun highlight(selectedView: android.view.View) {
            for ((v, _) in palettes) {
                v.alpha = if (v == selectedView) 1f else 0.4f
            }
        }
        highlight(palettes[0].first)
        for ((paletteView, hex) in palettes) {
            paletteView.setOnClickListener {
                chosenColour = hex
                highlight(paletteView)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("New category")
            .setView(view)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text?.toString()?.trim().orEmpty()
                //if they didnt add a name ask them for one
                if (name.isEmpty()) {
                    toast("Please enter a name")
                    return@setPositiveButton
                }
                createCategory(name, chosenColour)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createCategory(name: String, colour: String) {
        //fetch the user id
        val userId = Session.getUserId(this)
        //call on roomdb to use the doas
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@AddExpenseActivity)
                //add the new category to the db with current user id, cat name and colour
                val newId = db.categoryDao().insert(
                    CategoryEntity(userId = userId, name = name, colour = colour)
                )
                Log.d(TAG, "create category: '$name' ($colour) -> id=$newId")

                //refresh the list to include the new category
                availableCategories = db.categoryDao().getForUser(userId)
                selectedCategory = availableCategories.firstOrNull { it.id == newId }
                updateCategoryLabel()
                toast("Category created")
            } catch (e: Exception) {
                Log.e(TAG, "create category: error", e)
                toast("Could not create category")
            }
        }
    }

    private fun updateCategoryLabel() {
        val cat = selectedCategory
        if (cat == null) {
            binding.categoryValue.text = "Choose"
            //hide the dot by making it transparent (stops the dot from keeping the previous selected category colour)
            binding.categoryColourDot.background.setTint(0x00000000)
        } else {
            binding.categoryValue.text = cat.name
            try {
                val colour = android.graphics.Color.parseColor(cat.colour)
                binding.categoryColourDot.background.setTint(colour)
            } catch (e: Exception) {
                Log.w(TAG, "update category label: bad colour '${cat.colour}'")
            }
        }
    }

    //Photo stuff
    private fun launchCamera() {
        try {
            val photoFile = createPhotoFile()
            pendingPhotoFile = photoFile

            val photoUri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            Log.d(TAG, "launch camera: about to write to $photoFile (uri=$photoUri)")
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e(TAG, "launch camera: error", e)
            Toast.makeText(this, "Could not open the camera", Toast.LENGTH_SHORT).show()
        }
    }

    //create an empty unique file for the camera to write to
    private fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        // create the directory if it doesnt exist ie. new download
        storageDir?.mkdirs()
        return File.createTempFile("expense_${timestamp}_", ".jpg", storageDir)
    }

    private fun showPhotoPreview(file: File) {
        binding.photoPlaceholder.visibility = android.view.View.GONE
        binding.photoPreview.visibility = android.view.View.VISIBLE
        binding.photoRemoveButton.visibility = android.view.View.VISIBLE
        binding.photoPreview.setImageURI(Uri.fromFile(file))
    }

    private fun clearPhoto() {
        pendingPhotoFile?.delete()
        pendingPhotoFile = null
        savedPhotoPath = null
        binding.photoPreview.setImageURI(null)
        binding.photoPreview.visibility = android.view.View.GONE
        binding.photoRemoveButton.visibility = android.view.View.GONE
        binding.photoPlaceholder.visibility = android.view.View.VISIBLE
        Log.d(TAG, "clear ohoto: removed")
    }

    private fun attemptSave() {
        // error checking for amount, must be greater than zero
        val amountText = binding.amountInput.text.toString().trim()
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            toast("Please enter an amount greater than zero")
            return
        }
        //must have an associated category
        val category = selectedCategory
        if (category == null) {
            toast("Please choose a category")
            return
        }
        val note = binding.noteInput.text.toString().trim()
        val isRecurring = binding.recurringSwitch.isChecked

        Log.d(
            TAG,
            "attempted Save: amount=$amount cat=${category.id} date=$selectedDateMillis recurring=$isRecurring"
        )

        // Insert the user entered data into the database
        val userId = Session.getUserId(this)
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@AddExpenseActivity)
                val newId = db.expenseDao().insert(
                    ExpenseEntity(
                        userId = userId,
                        categoryId = category.id,
                        dateMillis = selectedDateMillis,
                        description = note,
                        amount = amount,
                        photoPath = savedPhotoPath,
                        isRecurring = isRecurring,
                        recurringSourceId = null
                    )
                )
                Log.d(TAG, "attempted save: inserted id=$newId")
                toast("Saved")
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "attempted save: error", e)
                toast("Could not save, please try again")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}