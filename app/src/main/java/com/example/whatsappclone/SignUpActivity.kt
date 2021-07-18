package com.example.whatsappclone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.example.whatsappclone.databinding.ActivitySignUpBinding
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask

private const val PERMISSION_READ = 1001
private const val PERMISSION_WRITE = 1002
private const val GALLERY_PHOTO = 1000

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    private val storage by lazy {
        FirebaseStorage.getInstance()
    }
    private val auth by lazy {
        FirebaseAuth.getInstance()
    }
    private val database by lazy {
        FirebaseFirestore.getInstance()
    }
    private lateinit var downloadUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.userImgView.setOnClickListener {
            checkPermissionForImage()
        }

        binding.nextBtn.setOnClickListener {
            val name = binding.nameEt.text.toString()
            if (!::downloadUrl.isInitialized) {
                toast("Photo cannot be empty")
            } else if (name.isEmpty()) {
                toast("Name cannot be empty")
            } else {
                val user = User(name, downloadUrl, downloadUrl, auth.uid!!)
                database.collection("users").document(auth.uid!!).set(user).addOnSuccessListener {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }.addOnFailureListener {
                    binding.nextBtn.isEnabled = true
                }
            }
        }
    }

    private fun checkPermissionForImage() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                    && (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            ) {
                val permissionRead = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                val permissionWrite = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

                requestPermissions(permissionRead, PERMISSION_READ)
                requestPermissions(permissionWrite, PERMISSION_WRITE)
            } else {
                pickImageFromGallery()
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_PHOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK && requestCode == GALLERY_PHOTO){
            data?.data?.let {
                binding.userImgView.setImageURI(it)
                startUpload(it)
            }
        }
    }

    private fun startUpload(filePath: Uri) {
        binding.nextBtn.isEnabled = false
        val ref = storage.reference.child("uploads/" + auth.uid.toString())
        val uploadTask = ref.putFile(filePath)
        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                downloadUrl = task.result.toString()
                binding.nextBtn.isEnabled = true
            } else {
                binding.nextBtn.isEnabled = true
                // Handle failures
            }
        }.addOnFailureListener {

        }
    }

    override fun onBackPressed() {

    }
}