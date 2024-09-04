package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

private const val AUTOCOMPLETE_REQUEST_CODE = 1

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private var selectedPlaceName: String? = null
    private var selectedLatLng: LatLng? = null  // 경도와 위도 정보를 저장하기 위한 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Places SDK 초기화
        Places.initialize(applicationContext, "AIzaSyCqC4aLoGhe6_Mv8KbFyD6rARFq9OsZ5EE")
        placesClient = Places.createClient(this)

        // MapFragment 설정
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 장소 검색 버튼 설정
        findViewById<Button>(R.id.searchButton).setOnClickListener {
            openAutocompleteActivity()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 초기 위치 설정 (예: 부산대학교 근처)
        val initialLocation = LatLng(35.231554, 129.084139)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f))

        // 마커 클릭 리스너 설정
        mMap.setOnMarkerClickListener { marker ->
            selectedPlaceName = marker.title
            selectedLatLng = marker.position  // 마커의 위치를 저장
            true
        }
    }

    // AutocompleteActivity를 열어 장소 검색 기능 제공
    private fun openAutocompleteActivity() {
        // 부산대학교 근처에 위치 편향 설정
        val bounds = RectangularBounds.newInstance(
            LatLng(35.2285, 129.0805), // 남서쪽 모서리
            LatLng(35.2345, 129.0875)  // 북동쪽 모서리
        )

        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setLocationBias(bounds) // 위치 편향 설정
            .build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(data!!)
            val latLng = place.latLng
            if (latLng != null) {
                mMap.clear()
                val marker = mMap.addMarker(MarkerOptions().position(latLng).title(place.name))
                selectedPlaceName = place.name
                selectedLatLng = latLng  // 검색한 장소의 위치를 저장
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                marker?.showInfoWindow()  // 마커의 정보 창을 표시
            }
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            val status = Autocomplete.getStatusFromIntent(data!!)
            status.statusMessage?.let { errorMessage ->
                println("Error: $errorMessage")
            }
        }
    }

    // 선택된 장소 이름과 위치를 PartnershipNoticeActivity로 전달하고 종료
    private fun returnToPartnershipNoticeActivity() {
        val resultIntent = Intent().apply {
            putExtra("place_name", selectedPlaceName)
            selectedLatLng?.let {
                putExtra("latitude", it.latitude)
                putExtra("longitude", it.longitude)
            }
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
    
}
