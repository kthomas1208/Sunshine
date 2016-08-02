# Sunshine with Wear Watchface
## (Go Ubiquitous!) 
Udacity Android Nanodegree Project 6

Author: Kevin Thomas

## Purpose
This app is for Udacity's Android Nanodegree. It contains the [Sunshine project](https://github.com/udacity/Sunshine-Version-2) as well as
a watch face compatible with Android Wear. Some concepts covered are:
* Using the DataLayer to send data from the mobile device to the wear device. 
* Accounting for UI differences between round and square wear devices
* Displaying a different face depending on if the wear device is in ambient or interactive mode

## Description
The app is a watchface that's automagically installed on an Android Wear device along with the mobile app. Once the watchface is set,
it will display the current time in military format, along with the date. It will also display the temperature in Celcius along with a
graphic showing the weather condition (e.g. raining, cloudy, sunny).

<img src="https://github.com/kthomas1208/Sunshine/blob/master/round.png" />
<img src="https://github.com/kthomas1208/Sunshine/blob/master/square.png"/>

### Running the app yourself
You'll need to download Android Studio or some other IDE and clone this repository from your version control system. 

## Libraries/References
* [Setting up environment for wearable development](https://developer.android.com/training/wearables/apps/creating.html)

### Stack Overflow/Forum references
These are the links to Stack Overflow pages and forum threads that helped me during the development process
* [Copying drawable resources for icons to wear project ](https://discussions.udacity.com/t/sharing-resources-between-wear-phone/159036/3)
* [Setting the DataItem for urgent delivery](https://discussions.udacity.com/t/sunshine-syncadapter-is-sending-data-but-wearable-cant-retrieve-it/171790)
* [Fixing ADB connectivity issues with actual physical device](http://stackoverflow.com/questions/23081263/adb-android-device-unauthorized/25546300#25546300)
