package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.FirebaseFirestore

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val LOCATION_PERMISSION_REQUEST_CODE = 1

class MapFragment : Fragment(), OnMapReadyCallback {

    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private val firestore = FirebaseFirestore.getInstance() // Firestore 인스턴스
    private var isLocationDialogShown = false // 위치 다이얼로그 중복 방지 플래그

    // 기본 위치를 부산대 위치로 설정
    private val defaultLocation = LatLng(35.231, 129.083) // 부산대 위치

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        // Places API 초기화
        Places.initialize(requireContext(), "")
        placesClient = Places.createClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // 지도 초기화
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 기본 위치를 부산대로 설정하고 초기 줌 레벨 설정
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

        // Firestore에서 위치 데이터를 가져와서 지도에 표시
        loadLocationsFromFirestore()

        // 현재 위치 업데이트 메서드 호출
        updateMap()
    }

    override fun onResume() {
        super.onResume()
        if (::mMap.isInitialized) {  // mMap이 초기화되었는지 확인
            updateMap()  // 위치 설정이 변경되었을 때 다시 위치를 업데이트
        } else {
            // mMap이 초기화되지 않았을 경우 처리
            // 이 상황은 거의 발생하지 않겠지만, 만약 발생하면 필요한 경우 로그를 남기거나 알림을 줄 수 있습니다.
            // 예: Log.w("MapFragment", "mMap is not initialized in onResume")
        }
    }

    // Firestore에서 위치 데이터를 가져와서 지도에 마커를 추가하는 메서드
    private fun loadLocationsFromFirestore() {
        firestore.collection("partnershipinfo") // 위치 데이터가 저장된 컬렉션 이름을 입력하세요.
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val geoPoint = document.getGeoPoint("location") // "location" 필드에서 위치 데이터를 가져옵니다.
                    val title = document.getString("title") // 가게 이름이나 위치의 제목을 가져옵니다.

                    if (geoPoint != null && title != null) {
                        val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(title)
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                // 오류 처리 (예: 로그 출력)
            }
    }

    // 사용자의 위치 정보로 지도를 업데이트하는 메서드
    private fun updateMap() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (isLocationEnabled()) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true

                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                        // 주변 장소 검색
                        fetchNearbyPlaces(currentLatLng)
                    }
                }.addOnFailureListener { exception ->
                    exception.printStackTrace()
                    Toast.makeText(requireContext(), "위치 정보를 가져오는 데 실패했습니다: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                showLocationSettingDialog() // 위치 설정 다이얼로그 표시
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Google Places API를 사용해 주변 장소 정보를 가져오는 메서드
    private fun fetchNearbyPlaces(latLng: LatLng) {
        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            placesClient.findCurrentPlace(request).addOnSuccessListener { response ->
                for (placeLikelihood in response.placeLikelihoods) {
                    val place = placeLikelihood.place
                    val placeLatLng = place.latLng
                    if (placeLatLng != null) {
                        mMap.addMarker(
                            MarkerOptions()
                                .position(placeLatLng)
                                .title(place.name)
                        )
                    }
                }
            }.addOnFailureListener { exception ->
                exception.printStackTrace()
                // 오류 처리 (예: 로그 출력)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    // 사용자의 위치 서비스 활성화 여부 확인 메서드
    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // 위치 서비스 설정 다이얼로그 표시 메서드
    private fun showLocationSettingDialog() {
        if (!isLocationDialogShown) {  // 다이얼로그가 이미 표시되었는지 확인
            isLocationDialogShown = true
            AlertDialog.Builder(requireContext())
                .setTitle("위치 서비스 비활성화")
                .setMessage("위치 서비스를 활성화해야 합니다. 위치 설정 화면으로 이동하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                    isLocationDialogShown = false // 다이얼로그가 닫히면 플래그 리셋
                }
                .setNegativeButton("아니오") { _, _ ->
                    isLocationDialogShown = false // 다이얼로그가 닫히면 플래그 리셋
                }
                .show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MapFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
