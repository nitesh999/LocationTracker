package com.location.tracking.worker

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import com.location.tracking.worker.databinding.ActivityMainBinding
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var mainBinding: ActivityMainBinding

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        //this.supportActionBar!!.hide()

        if (checkLocationPermission()) {
            scheduleWork()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                permission.ACCESS_COARSE_LOCATION
            ) ||
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                permission.ACCESS_FINE_LOCATION
            )
        ) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            //showInContextUI(...);
            showSnackbarMessage()
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    permission.ACCESS_COARSE_LOCATION,
                    permission.ACCESS_FINE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showSnackbarMessage() {
        Snackbar.make(findViewById(android.R.id.content), "Please Grant Permissions to access your location",
            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE") {
            requestPermissions(
                arrayOf(permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE
            )
        }.show()
    }


    private fun scheduleWork() {
        mainBinding.appCompatButtonStart.visibility = View.VISIBLE
        mainBinding.message.visibility = View.VISIBLE
        mainBinding.logs.visibility = View.VISIBLE
        try {
            if (isWorkScheduled(WorkManager.getInstance(this).getWorkInfosByTag(TAG).get())) {
                mainBinding.appCompatButtonStart.text = getString(R.string.button_text_stop)
                mainBinding.message.text = getString(R.string.message_worker_running)
                mainBinding.logs.text = getString(R.string.log_for_running)
            } else {
                mainBinding.appCompatButtonStart.text = getString(R.string.button_text_start)
                mainBinding.message.text = getString(R.string.message_worker_stopped)
                mainBinding.logs.text = getString(R.string.log_for_stopped)
            }
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        mainBinding.appCompatButtonStart.setOnClickListener {
            if (mainBinding.appCompatButtonStart.text.toString() == getString(R.string.button_text_start)) {
                // START Worker
                val periodicWork = PeriodicWorkRequest.Builder(MyWorker::class.java, 15, TimeUnit.MINUTES)
                    .addTag(TAG)
                    .build()
                WorkManager.getInstance(this).enqueueUniquePeriodicWork("Location",
                    ExistingPeriodicWorkPolicy.REPLACE, periodicWork)
                Toast.makeText(this@MainActivity, "Location Worker Started : " + periodicWork.id, Toast.LENGTH_SHORT).show()
                mainBinding.appCompatButtonStart.text = getString(R.string.button_text_stop)
                mainBinding.message.text = periodicWork.id.toString()
                mainBinding.logs.text = getString(R.string.log_for_running)
            } else {
                WorkManager.getInstance(this).cancelAllWorkByTag(TAG)
                mainBinding.appCompatButtonStart.text = getString(R.string.button_text_start)
                mainBinding.message.text = getString(R.string.message_worker_stopped)
                mainBinding.logs.text = getString(R.string.log_for_stopped)
            }
        }
    }

    private fun isWorkScheduled(workInfos: List<WorkInfo>?): Boolean {
        var running = false
        if (workInfos == null || workInfos.isEmpty()) return false
        for (workStatus in workInfos) {
            running = workStatus.state == WorkInfo.State.RUNNING || workStatus.state == WorkInfo.State.ENQUEUED
        }
        return running
    }

    /**
     * All about permission
     */
    private fun checkLocationPermission(): Boolean {
        val result3 =
            ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
        val result4 = ContextCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
        return result3 == PackageManager.PERMISSION_GRANTED && result4 == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                val coarseLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val fineLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (coarseLocation && fineLocation) Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT)
                    .show() else if (Build.VERSION.SDK_INT >= 23 && (!shouldShowRequestPermissionRationale(
                        permissions[0]
                    ) || !shouldShowRequestPermissionRationale(permissions[1]))
                ) {
                    Toast.makeText(this@MainActivity, "Go to Settings and Grant the permission to use this feature.", Toast.LENGTH_SHORT).show()
                    // User selected the Never Ask Again Option
                    navigateToSettings()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToSettings() {
        val i = Intent()
        i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        i.addCategory(Intent.CATEGORY_DEFAULT)
        i.data = Uri.parse("package:$packageName")
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        startActivity(i)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 200
        private const val TAG = "LocationUpdate"
    }
}
