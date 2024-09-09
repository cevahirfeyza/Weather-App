package com.example.weatherapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.weatherapp.POJO.ModelClass
import com.example.weatherapp.Utilities.ApiUtilities
import com.example.weatherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {


    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var activityMainBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        activityMainBinding=DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.rlMainLayout.visibility=View.GONE

        getCurrentLocation()

        activityMainBinding.etGetCityName.setOnEditorActionListener( { v, actionId, keyEvent ->

            if(actionId==EditorInfo.IME_ACTION_SEARCH){
                getCityWeather(activityMainBinding.etGetCityName.text.toString())
                val view = this.currentFocus
                if(view!=null){
                    val imm:InputMethodManager=getSystemService(INPUT_METHOD_SERVICE)as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken,0)
                    activityMainBinding.etGetCityName.clearFocus()
                }
                true
            }
            else false


        })




    }

    private fun getCityWeather(cityName: String) {

        activityMainBinding.pbLoading.visibility=View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(cityName, API_KEY)?.enqueue(object :Callback<ModelClass>
        {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
               setDataOnViews(response.body())
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext, "Not a valid city name", Toast.LENGTH_SHORT).show()


            }

        })

    }

    private fun getCurrentLocation(){

        if(checkPermissions()){

            if(isLocationEnabled()){

                //final location
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){ task->
                    val location: Location?=task.result
                    if(location==null){
                        Toast.makeText(this, "Null Received", Toast.LENGTH_SHORT).show()

                    }
                    else{
                        // fetch weather here
                        fetchCurrentLocationWeather(location.latitude.toString(), location.longitude.toString())

                    }

                }


            }
            else{
                //go to settings
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)


            }

        }
        else{

            //request permission
            requestPermission()

        }





    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {

        activityMainBinding.pbLoading.visibility=View.VISIBLE
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)?.enqueue(object : Callback<ModelClass> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                if(response.isSuccessful){
                    setDataOnViews(response.body())
                }
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext, "ERROR", Toast.LENGTH_SHORT).show()

            }

        })





    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body: ModelClass?) {

        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate =sdf.format(Date())
        activityMainBinding.tvDateAndTime.text=currentDate

        activityMainBinding.tvDayMaxTemp.text="Day "+kelvinToCelsius(body!!.main.temp_max) +"°"
        activityMainBinding.tvDayMinTemp.text="Night "+kelvinToCelsius(body!!.main.temp_min) +"°"
        activityMainBinding.tvTemp.text=""+kelvinToCelsius(body!!.main.temp) +"°"
        activityMainBinding.tvFeelsLike.text="Feels Like:"+kelvinToCelsius(body!!.main.feels_like) +"°"
        activityMainBinding.tvWeatherType.text=body.weather[0].main
        activityMainBinding.tvSunrise.text=timeStampToLocalDate(body.sys.sunrise.toLong())
        activityMainBinding.tvSunset.text=timeStampToLocalDate(body.sys.sunset.toLong())
        activityMainBinding.tvPressure.text=body.main.pressure.toString()
        activityMainBinding.tvHumidity.text=body.main.humidity.toString() +" %"
        activityMainBinding.tvWindSpeed.text= body.wind.speed.toString()+" m/s"
        activityMainBinding.tvTempFahrenheit.text=""+((kelvinToCelsius(body.main.temp)).times(1.8).plus(32).roundToInt())
        activityMainBinding.etGetCityName.setText(body.name)

        updateUI(body.weather[0].id)

    }

    private fun updateUI(id: Int) {

        if(id in 200..232){
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor((R.color.thunderstorm))
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.thunderstorm))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.thunderstormbg)

            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.thunderstormbg)
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.thunderstormbg)

            activityMainBinding.weatherBg.setImageResource(R.drawable.thunderstormbg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.thunderstorm1)

            //iv weatherbg diye 1 şey

        }
        else if(id in 300..321){

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor((R.color.drizzle))
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.drizzle))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.drizzlebg)

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.drizzlebg)
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.drizzlebg)


            activityMainBinding.weatherBg.setImageResource(R.drawable.drizzlebg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.drizzle)


        }
        else if(id in 500..531){

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor((R.color.rain))
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.rain))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.rainbg)

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.rainbg)
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.rainbg)


            activityMainBinding.weatherBg.setImageResource(R.drawable.rainbg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.rain)

        }
        else if(id in 600..620){

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor((R.color.snow))
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.snow))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.snowbg)

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.snowbg)
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.snowbg)


            activityMainBinding.weatherBg.setImageResource(R.drawable.snowbg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.snow)



        }
        else if(id in 701..781){
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor((R.color.mist))
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.mist))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.mistbg)

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.mistbg)
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.mistbg)


            activityMainBinding.weatherBg.setImageResource(R.drawable.mistbg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.mist)
        }
        else if(id==800){
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor((R.color.clear))
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clear))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.clearbg)

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.clearbg)
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.clearbg)


            activityMainBinding.weatherBg.setImageResource(R.drawable.clearbg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clear)

        }
        else{
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor((R.color.clouds))
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clouds))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.cloudsbg)

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.cloudsbg)
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.cloudsbg)


            activityMainBinding.weatherBg.setImageResource(R.drawable.cloudsbg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clouds)

        }

        activityMainBinding.pbLoading.visibility=View.GONE
        activityMainBinding.rlMainLayout.visibility=View.VISIBLE

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timeStamp: Long): String {
        val localTime = timeStamp.let {
            Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalTime()


        }
         return localTime.toString()
    }

    private fun kelvinToCelsius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()

    }

    private fun isLocationEnabled(): Boolean{
        val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }





    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_ACCESS_LOCATION)
    }


    companion object{
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val API_KEY = "0a90bf73e45bacd310c022467c2973f4"
    }

    private fun checkPermissions(): Boolean{
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true
        }

        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_REQUEST_ACCESS_LOCATION){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }
            else{
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()
            }
        }




    }

}