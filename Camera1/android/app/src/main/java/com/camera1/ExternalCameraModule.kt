package com.camera1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.facebook.react.bridge.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExternalCameraModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {

  private var pendingPromise: Promise? = null
  private var currentPhotoUri: Uri? = null

  companion object {
    private const val REQUEST_TAKE_PHOTO = 0xCA11 // arbitrary
    private const val TAG = "ExternalCameraModule"
  }

  init {
    reactContext.addActivityEventListener(this)
  }

  override fun getName(): String = "ExternalCamera"

  @ReactMethod
  fun openCamera(promise: Promise) {
    Log.d(TAG, "openCamera invoked")
    val activity = currentActivity
    if (activity == null) {
      Log.w(TAG, "openCamera aborted: currentActivity == null")
      promise.reject("NO_ACTIVITY", "Activity not available")
      return
    }
    if (pendingPromise != null) {
      Log.w(TAG, "openCamera aborted: pendingPromise already in progress")
      promise.reject("IN_PROGRESS", "Another camera request is in progress")
      return
    }

    val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
    // Create a temp file in the app's external pictures dir
    val picturesDir = reactContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    if (picturesDir == null) {
      Log.e(TAG, "External pictures directory not available")
      promise.reject("NO_DIR", "External pictures directory not available")
      return
    }
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val photoFile = File.createTempFile("IMG_${'$'}timeStamp", ".jpg", picturesDir)
    Log.d(TAG, "Temporary photo file created at: ${'$'}{photoFile.absolutePath}")
    val authority = reactContext.packageName + ".fileprovider"
    val photoUri = FileProvider.getUriForFile(reactContext, authority, photoFile)
    currentPhotoUri = photoUri
    Log.d(TAG, "Photo URI prepared: ${'$'}photoUri with authority ${'$'}authority")
    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)
    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

    val handler = intent.resolveActivity(reactContext.packageManager)
    if (handler == null) {
      Log.e(TAG, "No camera app available to handle ACTION_IMAGE_CAPTURE intent")
      promise.reject("NO_CAMERA", "No camera app available")
      return
    }
    Log.d(TAG, "Camera intent will be handled by component: ${'$'}handler")

    pendingPromise = promise
    try {
      Log.i(TAG, "Launching ACTION_IMAGE_CAPTURE with requestCode=${'$'}REQUEST_TAKE_PHOTO")
      activity.startActivityForResult(intent, REQUEST_TAKE_PHOTO)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch camera intent", e)
      pendingPromise = null
      currentPhotoUri = null
      promise.reject("LAUNCH_FAIL", e)
    }
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
    Log.d(TAG, "onActivityResult invoked with requestCode=${'$'}requestCode resultCode=${'$'}resultCode data=${'$'}data")
    if (requestCode != REQUEST_TAKE_PHOTO) {
      Log.d(TAG, "Ignoring activity result for requestCode=${'$'}requestCode")
      return
    }
    val promise = pendingPromise ?: run {
      Log.w(TAG, "Received camera result but pendingPromise is null")
      return
    }
    pendingPromise = null
    if (resultCode == Activity.RESULT_OK) {
      // Return the uri string. The image is written to this URI by the camera app.
      val uriStr = currentPhotoUri?.toString()
      Log.i(TAG, "Camera result OK, returning URI=${'$'}uriStr")
      currentPhotoUri = null
      promise.resolve(uriStr)
    } else {
      Log.w(TAG, "Camera result not OK (code=${'$'}resultCode), returning null")
      currentPhotoUri = null
      promise.resolve(null) // cancelled or failed
    }
  }

  override fun onNewIntent(intent: Intent?) {
    // no-op
  }
}
