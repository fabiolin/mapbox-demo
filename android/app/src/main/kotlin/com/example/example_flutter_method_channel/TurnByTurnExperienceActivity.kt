package com.example.example_flutter_method_channel
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.view.View
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.*
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
 import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton
import com.mapbox.navigation.ui.maps.camera.view.MapboxRouteOverviewButton
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.ui.voice.view.MapboxSoundButton
import com.mapbox.turf.TurfMeasurement.bearing
import com.mapbox.turf.TurfMeasurement.center
import java.util.Locale
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mapbox.api.directions.v5.DirectionsAdapterFactory
import com.mapbox.api.directions.v5.models.LegStep
class TurnByTurnExperienceActivity : AppCompatActivity() {
    private val tag = "TurnByTurnExperienceActivity"
    val mapLatLng = mutableListOf<LatLongMaps>()

    private companion object {
        private const val BUTTON_ANIMATION_DURATION = 1500L
    }

    /**
     * Debug tool used to play, pause and seek route progress events that can be used to produce mocked location updates along the route.
     */
    private val mapboxReplayer = MapboxReplayer()

    /**
     * Debug tool that mocks location updates with an input from the [mapboxReplayer].
     */
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)

    /**
     * Debug observer that makes sure the replayer has always an up-to-date information to generate mock updates.
     */
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    /**
     * Mapbox Maps entry point obtained from the [MapView].
     * You need to get a new reference to this object whenever the [MapView] is recreated.
     */
    private lateinit var mapboxMap: MapboxMap

    /**
     * Mapbox Navigation entry point. There should only be one instance of this object for the app.
     * You can use [MapboxNavigationProvider] to help create and obtain that instance.
     */
    private lateinit var mapboxNavigation: MapboxNavigation

    /**
     * Used to execute camera transitions based on the data generated by the [viewportDataSource].
     * This includes transitions from route overview to route following and continuously updating the camera as the location changes.
     */
    private lateinit var navigationCamera: NavigationCamera

    /**
     * Produces the camera frames based on the location and routing data for the [navigationCamera] to execute.
     */
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    /*
     * Below are generated camera padding values to ensure that the route fits well on screen while
     * other elements are overlaid on top of the map (including instruction view, buttons, etc.)
     */
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    /**
     * Generates updates for the [MapboxManeuverView] to display the upcoming maneuver instructions
     * and remaining distance to the maneuver point.
     */
    private lateinit var maneuverApi: MapboxManeuverApi

    /**
     * Generates updates for the [MapboxTripProgressView] that include remaining time and distance to the destination.
     */
    private lateinit var tripProgressApi: MapboxTripProgressApi

    /**
     * Generates updates for the [routeLineView] with the geometries and properties of the routes that should be drawn on the map.
     */
    private lateinit var routeLineApi: MapboxRouteLineApi

    /**
     * Draws route lines on the map based on the data from the [routeLineApi]
     */
    private lateinit var routeLineView: MapboxRouteLineView

    /**
     * Generates updates for the [routeArrowView] with the geometries and properties of maneuver arrows that should be drawn on the map.
     */
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()

    /**
     * Draws maneuver arrows on the map based on the data [routeArrowApi].
     */
    private lateinit var routeArrowView: MapboxRouteArrowView


    /**
     * Stores and updates the state of whether the voice instructions should be played as they come or muted.
     */
    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            if (value) {
                findViewById<MapboxSoundButton>(R.id.soundButton).muteAndExtend(
                    BUTTON_ANIMATION_DURATION
                )
                voiceInstructionsPlayer.volume(SpeechVolume(0f))
            } else {
                findViewById<MapboxSoundButton>(R.id.soundButton).unmuteAndExtend(
                    BUTTON_ANIMATION_DURATION
                )
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }

    /**
     * Extracts message that should be communicated to the driver about the upcoming maneuver.
     * When possible, downloads a synthesized audio file that can be played back to the driver.
     */
    private lateinit var speechApi: MapboxSpeechApi

    /**
     * Plays the synthesized audio files with upcoming maneuver instructions
     * or uses an on-device Text-To-Speech engine to communicate the message to the driver.
     */
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    /**
     * Observes when a new voice instruction should be played.
     */
    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    /**
     * Based on whether the synthesized audio file is available, the callback plays the file
     * or uses the fall back which is played back using the on-device Text-To-Speech engine.
     */
    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    // play the instruction via fallback text-to-speech engine
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    // play the sound file from the external generator
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    /**
     * When a synthesized audio file was downloaded, this callback cleans up the disk after it was played.
     */
    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
            // remove already consumed file to free-up space
            speechApi.clean(value)
        }

    /**
     * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
     * to the Maps SDK in order to update the user location indicator on the map.
     */
    private val navigationLocationProvider = NavigationLocationProvider()

    var tokenMapBox = "YOU-ACCESS-TOKEN-HERE"

    /**
     * Gets notified with location updates.
     *
     * Exposes raw updates coming directly from the location services
     * and the updates enhanced by the Navigation SDK (cleaned up and matched to the road).
     */
    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {
            // not handled

        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            // update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            // update camera position to account for new location
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()

            // if this is the first location update the activity has received,
            // it's best to immediately move the camera to the current user location
            if (!firstLocationUpdateReceived) {
                firstLocationUpdateReceived = true
                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0) // instant transition
                        .build()
                )
            }
        }
    }

    /**
     * Gets notified with progress along the currently active route.
     */
    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        // update the camera position to account for the progressed fragment of the route
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        // draw the upcoming maneuver arrow on the map
        val style = mapboxMap.getStyle()
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }

        // update top banner with maneuver instructions
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold(
            { error ->
                Toast.makeText(
                    this@TurnByTurnExperienceActivity,
                    error.errorMessage,
                    Toast.LENGTH_SHORT
                ).show()
            },
            {
                findViewById<MapboxManeuverView>(R.id.maneuverView).visibility = View.VISIBLE
                findViewById<MapboxManeuverView>(R.id.maneuverView).renderManeuvers(maneuvers)
            }
        )

        // update bottom trip progress summary
        findViewById<MapboxTripProgressView>(R.id.tripProgressView).render(
            tripProgressApi.getTripProgress(routeProgress)
        )
        //Notify to Flutter UI about the time remaining
     /*   val gson: Gson = GsonBuilder().registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create()

*//*        val directionsRoute: DirectionsRoute = routeProgress.route.get(0).directionsRoute
        val gson: Gson = GsonBuilder().registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create()
        val stringJS: String = gson.toJson(directionsRoute)
        val parsedFromJsonRoute = gson.fromJson(stringJS, DirectionsRoute::class.java)*//*
        var  stepLists = routeProgress.route.legs()!![0].steps()
        val stepJSON: String = gson.toJson(stepLists)
        Log.v("!!!!!!!!!!!!!!!!",stepJSON)

        for (i in stepLists!!.indices) {
            var ll=LatLongMaps()
            ll.lat=stepLists[i].maneuver().location().latitude()
            ll.lng=stepLists[i].maneuver().location().longitude()
            mapLatLng.add(ll)
        }

*/

       val eventChannelHandler = EventChannelHandler(context = applicationContext)
        var data=InitialRouteData()
        data.type="eta"
        data.latLngJSON=""
        data.eta=(routeProgress.durationRemaining/60).toString()
        //eventChannelHandler.onReportGameClicked(applicationContext, (routeProgress.durationRemaining/60).toString())
        eventChannelHandler.onReportGameClicked(applicationContext, Gson().toJson(data))


    }


    /**
     * Gets notified whenever the tracked routes change.
     *
     * A change can mean:
     * - routes get changed with [MapboxNavigation.setRoutes]
     * - routes annotations get refreshed (for example, congestion annotation that indicate the live traffic along the route)
     * - driver got off route and a reroute was executed
     */
    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.routes.isNotEmpty()) {
            // generate route geometries asynchronously and render them
            val routeLines = routeUpdateResult.routes.map { RouteLine(it, null) }

            routeLineApi.setRoutes(
                routeLines
            ) { value ->
                mapboxMap.getStyle()?.apply {
                    routeLineView.renderRouteDrawData(this, value)
                }
            }

            // update the camera position to account for the new route
            viewportDataSource.onRouteChanged(routeUpdateResult.routes.first())
            viewportDataSource.evaluate()
        } else {
            // remove the route line and route arrow from the map
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

            // remove the route reference from camera position evaluations
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }
    var changePointLat = 0.0
    var changePointLng = 0.0
    var startPointLat = 0.0
    var startPointLon = 0.0
    var endPointLat = 0.0
    var endPointLong = 0.0


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turn_by_turn_experience)
        supportActionBar!!.title = "Map Demo"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        mapboxMap = findViewById<MapView>(R.id.mapView).getMapboxMap()


        // initialize the location puck
        findViewById<MapView>(R.id.mapView).location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@TurnByTurnExperienceActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }


        // initialize Mapbox Navigation
        mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(this.applicationContext)
                    .accessToken(tokenMapBox)
                    // comment out the location engine setting block to disable simulation
                    // .locationEngine(replayLocationEngine)
                    .build()
            )
        }

        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        navigationCamera = NavigationCamera(
            mapboxMap,
            findViewById<MapView>(R.id.mapView).camera,
            viewportDataSource
        )
        // set the animations lifecycle listener to ensure the NavigationCamera stops
        // automatically following the user location when the map is interacted with
        findViewById<MapView>(R.id.mapView).camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
        navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            // shows/hide the recenter button depending on the camera state
            when (navigationCameraState) {
                NavigationCameraState.TRANSITION_TO_FOLLOWING,
                NavigationCameraState.FOLLOWING -> findViewById<MapboxRecenterButton>(R.id.recenter).visibility =
                    View.INVISIBLE
                NavigationCameraState.TRANSITION_TO_OVERVIEW,
                NavigationCameraState.OVERVIEW,
                NavigationCameraState.IDLE -> findViewById<MapboxRecenterButton>(R.id.recenter).visibility =
                    View.VISIBLE
            }
        }
        // set the padding values depending on screen orientation and visible view layout
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
        }
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.followingPadding = followingPadding
        }

        // make sure to use the same DistanceFormatterOptions across different features
        val distanceFormatterOptions = mapboxNavigation.navigationOptions.distanceFormatterOptions

        // initialize maneuver api that feeds the data to the top banner maneuver view
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        // initialize bottom progress view
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(this)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(
                    TimeRemainingFormatter(this)
                )
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                )
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(this, TimeFormat.NONE_SPECIFIED)
                )
                .build()
        )

        // initialize voice instructions api and the voice instruction player
        speechApi = MapboxSpeechApi(
            this,
            tokenMapBox,
            Locale.US.language
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            this,
            tokenMapBox,
            Locale.US.language
        )

        // initialize route line, the withRouteLineBelowLayerId is specified to place
        // the route line below road labels layer on the map
        // the value of this option will depend on the style that you are using
        // and under which layer the route line should be placed on the map layers stack
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label")
            .build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        // initialize maneuver arrow view to draw arrows on the map
        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)


//        val p =Point()
//        // load map style
//        mapboxMap.loadStyleUri(
//            Style.MAPBOX_STREETS
//        ) {
//            // add long click listener that search for a route to the clicked destination
//            findViewById<MapView>(R.id.mapView).gestures.addOnMapLongClickListener { point ->
//                findRoute(point)
//                true
//            }
//        }

        // initialize view interactions
        findViewById<ImageView>(R.id.stop).setOnClickListener {
            clearRouteAndStopNavigation()
        }
        findViewById<MapboxRecenterButton>(R.id.recenter).setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
            findViewById<MapboxRouteOverviewButton>(R.id.routeOverview).showTextAndExtend(
                BUTTON_ANIMATION_DURATION
            )
        }
        findViewById<MapboxRouteOverviewButton>(R.id.routeOverview).setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            findViewById<MapboxRecenterButton>(R.id.recenter).showTextAndExtend(
                BUTTON_ANIMATION_DURATION
            )
        }
        findViewById<MapboxSoundButton>(R.id.soundButton).setOnClickListener {
            // mute/unmute voice instructions
            isVoiceInstructionsMuted = !isVoiceInstructionsMuted
        }

        // set initial sounds button state
        findViewById<MapboxSoundButton>(R.id.soundButton).unmute()

        // start the trip session to being receiving location updates in free drive
        // and later when a route is set also receiving route progress updates
        mapboxNavigation.startTripSession()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() === android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()


       // Receive lat long data from main activity
        changePointLat = intent.getDoubleExtra("changePointLat", 0.0)
        changePointLng = intent.getDoubleExtra("changePointLng", 0.0)

        startPointLat = intent.getDoubleExtra("startPointLat", 0.0)
        startPointLon = intent.getDoubleExtra("startPointLon", 0.0)
        endPointLat = intent.getDoubleExtra("endPointLat", 0.0)
        endPointLong = intent.getDoubleExtra("endPointLong", 0.0)
        if(changePointLng!=null && changePointLng!=0.0){
            startPointLat=changePointLat
            startPointLon=changePointLng
        }


        // register event listeners
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)

        if (mapboxNavigation.getNavigationRoutes().toDirectionsRoutes().isEmpty()) {
            // if simulation is enabled (ReplayLocationEngine set to NavigationOptions)
            // but we're not simulating yet,
            // push a single location sample to establish origin
            mapboxReplayer.pushEvents(
                listOf(
                    ReplayRouteMapper.mapToUpdateLocation(
                        eventTimestamp = 0.0,
                        point = Point.fromLngLat(startPointLon, startPointLat)
                    )
                )
            )
            mapboxReplayer.playFirstLocation()
            //var originPoint:Point=null
         /*   if(changePointLng!=null && changePointLng!=0.0){
              val  originPoint = Point.fromLngLat(changePointLat, changePointLng)
                mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
                    routeLineView.initializeLayers(style)
                    findRoute(originPoint)
                }
            }else{

            }*/
            val  originPoint = Point.fromLngLat(endPointLong, endPointLat)
            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
                routeLineView.initializeLayers(style)
                findRoute(originPoint)
            }


        }
    }

    override fun onStop() {
        super.onStop()

        // unregister event listeners to prevent leaks or unnecessary resource consumption
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        MapboxNavigationProvider.destroy()
        mapboxReplayer.finish()
        maneuverApi.cancel()
        routeLineApi.cancel()
        routeLineView.cancel()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    private fun findRoute(destination: Point) {
        val originLocation = navigationLocationProvider.lastLocation
        val originPoint = originLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return

        // execute a route request
        // it's recommended to use the
        // applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
        // that make sure the route request is optimized
        // to allow for support of all of the Navigation SDK features
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(listOf(originPoint, destination))
                // provide the bearing for the origin of the request to ensure
                // that the returned route faces in the direction of the current user movement
                .bearingsList(
                    listOf(
                        Bearing.builder()
                            .angle(originLocation.bearing.toDouble())
                            .degrees(45.0)
                            .build(),
                        null
                    )
                )
                .layersList(listOf(mapboxNavigation.getZLevel(), null))
                .build(),
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    setRouteAndStartNavigation(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    // no impl
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
                }
            }
        )
    }

    private fun setRouteAndStartNavigation(routes: List<DirectionsRoute>) {
        // set routes, where the first route in the list is the primary route that
        // will be used for active guidance
        mapboxNavigation.setRoutes(routes)

        // start location simulation along the primary route
//        startSimulation(routes.first())

        // show UI elements
        findViewById<MapboxSoundButton>(R.id.soundButton).visibility = View.VISIBLE
        findViewById<MapboxRouteOverviewButton>(R.id.routeOverview).visibility = View.VISIBLE
        findViewById<CardView>(R.id.tripProgressCard).visibility = View.VISIBLE

        // move the camera to overview when new route is available
        mapboxMap.flyTo(
            cameraOptions {
                center(Point.fromLngLat(startPointLon, startPointLat)) // Sets the new camera position on click point
                zoom(18.0) // Sets the zoom
                bearing(180.0) // Rotate the camera
                pitch(50.0) // Set the camera pitch
            },
            mapAnimationOptions {
                duration(7000)
            }
        )

        val directionsRoute: DirectionsRoute = routes.get(0)
        val gson: Gson = GsonBuilder().registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create()
        val stringJS: String = gson.toJson(directionsRoute)
        val parsedFromJsonRoute = gson.fromJson(stringJS, DirectionsRoute::class.java)
        var  stepLists = parsedFromJsonRoute.legs()!!.get(0).steps()
        val stepJSON: String = gson.toJson(stepLists)
        print(stepJSON)

        for (i in stepLists!!.indices) {
            var ll=LatLongMaps()
            ll.lat=stepLists[i].maneuver().location().latitude()
            ll.lng=stepLists[i].maneuver().location().longitude()
            mapLatLng.add(ll)
        }
        val stepJSON1: String = gson.toJson(mapLatLng)

       Log.v("=======",stepJSON1)
        var data=InitialRouteData()
        data.type="initial_route"
        data.eta=""
        data.latLngJSON=stepJSON1


         val eventChannelHandler = EventChannelHandler(context = applicationContext)
        eventChannelHandler.onInitialRouteClicked(applicationContext, gson.toJson(data))

    }


    private fun clearRouteAndStopNavigation() {
        // clear
        mapboxNavigation.setRoutes(listOf())

        // stop simulation
        mapboxReplayer.clearEvents()
        mapboxReplayer.stop()

        // hide UI elements
        findViewById<MapboxSoundButton>(R.id.soundButton).visibility = View.INVISIBLE
        findViewById<MapboxManeuverView>(R.id.maneuverView).visibility = View.INVISIBLE
        findViewById<MapboxRouteOverviewButton>(R.id.routeOverview).visibility = View.INVISIBLE
        findViewById<CardView>(R.id.tripProgressCard).visibility = View.INVISIBLE
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.run {
            stop()
            clearEvents()
            val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
            pushEvents(replayEvents)
            seekTo(replayEvents.first())
            play()
        }
    }
}
