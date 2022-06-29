# GNSS_DR

GNSS_DR tracks and logs global navigation satellite systems (GNSS) 

## Features
### GPS Features
1. Ability to track GPS information via [GNSS](https://developer.android.com/guide/topics/sensors/gnss)
    - Latitude, Longitude, Speed, Height, # of Sats, Bearing, Horizontal Accuracy, Vertical Accuracy, Speed Accuracy
    - Individual Satellites (ID, GNSS Type, Elevation, Azim, C/NO)
2. Ability to track GPS information via [FusedLocationProvider](https://developers.google.com/location-context/fused-location-provider)
    - Latitude, Longitude, Speed, Height, Bearing, Horizontal Accuracy, Vertical Accuracy, Speed Accuracy
3. Uses GNSS by default and swtiches to FusedLocationProvider when gps fix is unavailable
4. Ability to choose GPS update frequency (in milliseconds)

### Logging Features
1. Logs GPS information in CSV format to phone's Internal Storage folder

## System Requirements
### Android Device with [support for GNSS](https://docs.google.com/spreadsheets/d/1z6Yt9c4cyev1PB6VWEkbZtJGfoxAQ5UJnHyP24sFwlk/edit#gid=0)
- Target SDK: API 30 (Android 11)
- Minimum SDK: API 28 (Android 9)

### Software
- Android Studio Bumblebee

### API Requirements
- Google Maps API Key (add to google_maps_api.xml)

## How to Use
- When you first launch the application after building the source code, you will see a Settings tab.
    - By default, the gps update frequency is set to 100ms but you can change this value with the slider
- If you then switch to the log tab, you will see that the gps button at the top right is switched off
    - Turn this switch button on to start tracking gps location
    - As mentioned above, the default location manager provider is GNSS but will automatically switch to FusedLocationProvider in a DR scenario
- You can use the start, reset, and save buttons in the bottom of the UI to log gps information accordingly
- A swipe up map that displays the currently tracked latitutde and longitude of the phone is included to check your general location

## Authors
- Hawon Park
- Jeong Ho Shin
