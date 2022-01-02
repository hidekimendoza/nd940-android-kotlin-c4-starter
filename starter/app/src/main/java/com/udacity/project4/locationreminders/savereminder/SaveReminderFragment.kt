package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val GEOFENCE_RADIUS_METERS = 150f
private const val TAG = "SaveReminderFragment"


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private var isSystemLocationEnabled = false

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        enableGeofencePermissions()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {

            val permissionGranted = isPermissionGranted()
            if (permissionGranted) {
                //             1) add a geofencing request
                //             2) save the reminder to the local db
                checkDeviceLocationSettingsAndStoreGeofence()
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.permission_select_denied_explanation, Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @SuppressLint("MissingPermission")
    private fun addReminderGeofence(reminderId: String) {
        if (_viewModel.latitude.value == null || _viewModel.longitude.value == null || _viewModel.reminderTitle == null) {
            Log.d("Geofence", "invalid latitude, longitude or title unable to create geofence")
            return
        }
        Log.d("Geofence", "valid latitude, longitude and title proceeding to build geofence")

        val geofence = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(reminderId)

            // Set the circular region of this geofence.
            .setCircularRegion(
                _viewModel.latitude.value!!,
                _viewModel.longitude.value!!,
                GEOFENCE_RADIUS_METERS
            )

            // Set the expiration duration of the geofence. This geofence gets automatically
            // removed after this period of time.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)

            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)

            // Create the geofence.
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        Log.i("AddGeofences", "Adding geofence with id: $reminderId")

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.i("AddGeofences", "Geofences addedOn succeeded")

                // Geofences added
                // ...
            }
            addOnFailureListener {
                // Failed to add geofences
                Log.i("AddGeofences", "Geofences addedOn Failed")

                // ...
            }
        }
    }

    @TargetApi(29)
    private fun isPermissionGranted(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun enableGeofencePermissions() {
        // Check if permissions for location are already granted
        if (isPermissionGranted()) {
            Log.i(TAG, "location permissions already granted")
            return
        }
        // In case location permissions are not granted, request permissions
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d("ReminderListFragment", "Request location permissions")
        requestPermissions(permissionsArray, resultCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                requireView(),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_LONG
            ).show()
//                .setAction(R.string.settings) {
//                    startActivity(Intent().apply {
//                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
//                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    })
//                }.show()
        }
    }

    private fun checkDeviceLocationSettingsAndStoreGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.saveReminderContainer,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStoreGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                val reminderData = ReminderDataItem(
                    title = _viewModel.reminderTitle.value,
                    description = _viewModel.reminderDescription.value,
                    location = _viewModel.reminderSelectedLocationStr.value,
                    latitude = _viewModel.latitude.value,
                    longitude = _viewModel.longitude.value
                )
                addReminderGeofence(reminderData.id)
                _viewModel.validateAndSaveReminder(reminderData)
            }
        }
    }


    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "com.udacity.project4.locationreminders.savereminder.SaveReminderFragment.action.ACTION_GEOFENCE_EVENT"


        internal const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        internal const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        internal const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        internal const val LOCATION_PERMISSION_INDEX = 0
        internal const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    }
}
