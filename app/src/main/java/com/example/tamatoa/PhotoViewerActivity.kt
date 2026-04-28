package com.example.tamatoa

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.tamatoa.databinding.ActivityPhotoViewerBinding
import java.io.File

//display attached photos fullscreen
//information from: https://www.youtube.com/watch?v=6T9v-4Gp9Bw
class PhotoViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PHOTO_PATH = "photo_path"
        private const val TAG = "PhotoViewerActivity"
    }

    private lateinit var binding: ActivityPhotoViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra(EXTRA_PHOTO_PATH)
        val file = File(path)
        if (!file.exists()) {
            Log.w(TAG, "photo not found: $path")
            finish()
            return
        }

        binding.photoView.setImageURI(Uri.fromFile(file))
        binding.closeButton.setOnClickListener { finish() }
    }
}