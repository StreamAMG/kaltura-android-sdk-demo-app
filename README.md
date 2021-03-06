:heavy_exclamation_mark: Please note this project is **deprecated**, please see [streamamg-sdk-android](https://github.com/StreamAMG/streamamg-sdk-android).

# StreamAMG Android test app #
---

![Picture](http://mp.streamamg.com/start/img/streamuk-logo.png) ![Picture](http://files.softicons.com/download/computer-icons/android-smartphones-icons-by-abhi-aravind/png/128x128/Android%20Logo.png)


---
## :wrench: Requisites ##

* Android Studio 3.0+ :computer:
* Android Emulator and/or real Android device :iphone:


---
## :warning: Android compatibility ##

* API 21+: Android 5.0 Lollipop


---
## :checkered_flag: Getting Started ##

1. First of all, you have to clone this repository on your local project directory.

    Open a terminal and move to the folder of your projects and then clone the StreamHub repository using `YOUR_USERNAME`:

        git clone https://YOUR_USERNAME@bitbucket.org/sukdev/streamamg-android-app.git (optional dest directory)

    A new folder should be created with name streamamg-android-app (if you didn't specify one)

2. Move to the new directory.

    Add **PlayerSDK** and **GoogleMediaFramework** (_Kaltura Android SDK_) into that directory:

        git clone https://YOUR_USERNAME@bitbucket.org/sukdev/kaltura-android-sdk.git (optional same directory)

    So the directories should be like this:

    > :open_file_folder: Projects Directory
    > > :file_folder:  streamamg-android-app
    > > > :file_folder:  kaltura-android-sdk
    > > > > :file_folder:  playerSDK
    > > > >
    > > > > :file_folder:  googlemediaframework
    > > > >
    > > > > _...Other files..._
    > > >
    > > > :file_folder:  app
    > > >
    > > > :file_folder:  gradle
    > > >
    > > > :file_folder:  fastlane
    > > >
    > > > _...Other files..._


3. Open Android Studio and select `streamamg-android-app` directory.

    The project should run in any emulator as well as in any real device with API >= 21


---
## :pencil: Credits ##

> *Author: [Stefano Russello](mailto:stefano.russello@streamamg.com)*
>
> *Date first version: 24/09/2018*
>
> *Updated:*
