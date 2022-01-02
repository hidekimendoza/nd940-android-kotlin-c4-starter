package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val DEFAULT_ZOOM = 15f

// SIDNEY
private val DEFAULT_LOCATION = LatLng(-34.0, 151.0)

class SelectLocationFragment : BaseFragment() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap
    private var lastKnownLocation: Location? = null
    private var selectedLocation: LatLng? = null
    private var selectedPoi: PointOfInterest? = null
    private var permissionDenied = true

    @SuppressLint("MissingPermission")
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                permissionDenied = false
                getDeviceLocation()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Toast.makeText(
                    requireContext(),
                    R.string.permission_denied_explanation,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        binding.buttonSaveLocation.setOnClickListener {
            onLocationSelected()
        }
        return binding.root
    }

    private fun onLocationSelected() {
        _viewModel.selectedPOI.value = selectedPoi
        _viewModel.latitude.value = selectedLocation?.latitude
        _viewModel.longitude.value = selectedLocation?.longitude
        _viewModel.reminderSelectedLocationStr.value = selectedPoi?.name
        Log.i("TAG", "POI/location saved")
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)

    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        Log.i("OnMapReadyCallback", "Map is available")
        map = googleMap
        setMapStyle(map)
        setMapLongClick(map)
        setPoiClick(map)
        getDeviceLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            enableMyLocation()
            map.isMyLocationEnabled = !permissionDenied
            if (!permissionDenied) {
                map.isMyLocationEnabled = !permissionDenied
                map.uiSettings.isMyLocationButtonEnabled = !permissionDenied
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM
                                )
                            )
                        }
                    } else {
                        Log.d("TAG", "Current location is null. Using defaults.")
                        Log.e("TAG", "Exception: %s", task.exception)
                        map.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
                        )
                        map.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.permission_denied_explanation,
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            selectedLocation = poi.latLng
            selectedPoi = poi
            map.clear()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(selectedLocation!!)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()
            Log.i("TAG", "Marker added at poi: $poi")

        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            selectedLocation = latLng
            selectedPoi = PointOfInterest(latLng, "User defined", "User defined")
            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(selectedLocation!!)
            )
            Log.i("TAG", "Marker added at location: $latLng")
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e("TAG", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("TAG", "Can't find style. Error: ", e)
        }
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                permissionDenied = false
                // You can use the API that requires the permission.
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                permissionDenied = false
                Log.i("enableMyLocation", "Fine location access requested")

            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }
}
