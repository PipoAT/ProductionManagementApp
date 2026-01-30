# Production Management Application

## Overview
- **Version: 2.0.0**
- The Production Management Application is a software developed to allow teams to view documentation and record notes with ease.
- **New in 2.0:** Updated technology stack, improved security, configurable server endpoints, and broader device compatibility.

## Table of Contents
1. [What's New in 2.0](#whats-new-in-20)
2. [Device Requirements](#device-requirements)
3. [Adobe Acrobat Reader](#adobe-acrobat-reader--required-)
4. [Configuration](#configuration)
5. [Upload/Update App](#uploadupdate-app)
6. [Frontend Layout](#frontend-layout)
7. [Backend Layout](#backend-layout)
8. [TeamsAPI/Workflows](#teamsapiworkflow)
9. [Documentation Naming Convention](#documentation-naming-convention)
10. [Thumbnail Naming Convention](#thumbnail-naming-convention)
11. [HTTP Server](#serverpy)
12. [Migration from 1.x](#migration-from-1x-to-20)
13. [Credits](#credits)

## What's New in 2.0
### Major Updates
- ✅ **Broader Device Support**: Now supports Android 8.0 (API 26) and newer (previously Android 14 only)
- ✅ **Updated Tech Stack**: 
  - Kotlin 1.9.22 (stable, from beta 1.9.0-Beta)
  - OkHttp 4.12.0 (stable, from alpha 5.0.0-alpha.11)
  - Latest AndroidX libraries
  - Room Database support added
  - KSP instead of kapt for faster builds
- ✅ **Improved Security**:
  - Network security config (restricted cleartext traffic to specific domains)
  - ProGuard rules for release builds with minification enabled
  - Configurable server endpoints via BuildConfig
- ✅ **Code Quality**:
  - Removed hardcoded server URLs
  - Replaced deprecated GlobalScope with proper coroutine scoping
  - Updated Java target from 1.8 to 17
- ⚠️ **Teams Integration**: Temporarily disabled due to Microsoft Teams Workflow API retirement (August 2024)
  - Notes marked as "issues" are now logged locally
  - Future versions will implement Microsoft Graph API integration

### Breaking Changes
- Server URL must now be configured in build.gradle (see [Configuration](#configuration))
- Teams webhook notifications are disabled (will be replaced with Graph API in future update)

## Device Requirements
- **Android OS Version:**
  - **Android 8.0 (API 26) or newer** (expanded from Android 14 only in v1.x)
  - Recommended: Android 10 or newer for best performance
- **Permissions:**
  - Permission to use device camera
  - Permission to access storage
  - Permission to access Adobe Acrobat Reader installed on device (see below)
  - Permission to access internet via wifi or internal network
  - ~~Permission to access Microsoft Teams~~ (temporarily disabled in v2.0)
- **Device Settings (Optional):**
  - Orientation/Auto-rotation preferably to be locked
  - Taskbar settings adjusted preferably to be minimal buttons only

## Adobe Acrobat Reader (REQUIRED)
- Prior to launching the app for the first time, Adobe Acrobat must be installed on the device.
  - Search for "Adobe PDF" in the Google Play Store
  - Locate "Adobe Acrobat Reader: Edit PDF" and install the app
  - Launch the app to bypass login and to ensure the application is working as intended
- On first launch of the ATech Training Production Management App, and on first click of any document to open:
  - Select the Adobe Acrobat App when the popup appears and select "Always" to ensure Adobe launches every time
  - Within Adobe, clicking on the triple dots -> pages -> contents will access bookmarks within Adobe

## Configuration
### Server Endpoint Configuration
Version 2.0 introduces configurable server endpoints. To change the server URL:

1. Open `app/build.gradle`
2. Locate the `defaultConfig` section
3. Modify the `SERVER_BASE_URL` field:
   ```gradle
   defaultConfig {
       ...
       buildConfigField "String", "SERVER_BASE_URL", "\"http://YOUR_SERVER_IP:PORT\""
   }
   ```
4. For debug builds, you can override in the `debug` buildType:
   ```gradle
   buildTypes {
       debug {
           buildConfigField "String", "SERVER_BASE_URL", "\"http://192.168.1.100:1025\""
       }
   }
   ```

### Network Security
Version 2.0 includes improved network security. If you need to add additional server IP addresses:

1. Open `app/src/main/res/xml/network_security_config.xml`
2. Add your server domain/IP:
   ```xml
   <domain includeSubdomains="false">YOUR_SERVER_IP</domain>
   ```

## Upload/Update App
- Download Android Studio if not available on your laptop/desktop
- Launch Android Studio
- At the top of Android Studio, click on the devices dropdown to display the devices menu.
- Select "Pair Devices using Wi-Fi", to which a QR code appears
- On the tablet/mobile device, open settings and scroll down to developer settings
  - If you do not see developer settings, go to the "About" settings and click on the build number 7 times to activate developer mode
  - If it does not appear and developer mode is active, utilize the search bar
- On the tablet/mobile device, select "Wireless Debugging" from the developer settings
- On the tablet/mobile device, select "Pair using QR code"
- Use the tablet to scan the QR code on the laptop/desktop
- In Android Studio, once the green checkmark shows (which may take up to 2 minutes to connect and show), launch the app by clicking the green play arrow.

## Frontend Layout
- Dashboard ("Manuals") Page
  - The home page of the application to where documentation can be located and opened
- Notes Page
  - The page that displays all user notes, with the ability to:
    - Record and modify records of notes

## Backend Layout
- NOTES TABLE

| Data                                 | Name          |
|--------------------------------------|---------------|
| INTEGER, PRIMARY KEY, AUTO INCREMENT | NID           |
| TEXT                                 | note_owner    |
| DATE                                 | date          |
| TEXT                                 | note          |
| TEXT                                 | uri           |
| TEXT                                 | image_comment |
| BOOLEAN                              | is_issue      |
| TEXT                                 | trainer       |
| TEXT                                 | page          |

## TeamsAPI/Workflow
### ⚠️ **Important: Teams Integration Temporarily Disabled in v2.0**
- **Background**: The Microsoft Teams Workflow API used in v1.x was retired by Microsoft on August 15, 2024.
- **Current Behavior**: Notes marked as "issues" are now logged locally in the application logs instead of being sent to Teams.
- **Future Plans**: Version 2.1+ will implement Microsoft Graph API with OAuth2 authentication for Teams integration.
  - See: [Microsoft Graph API - Send message to channel](https://learn.microsoft.com/en-us/graph/api/channel-post-messages)
- **Temporary Workaround**: Check application logs for issue notifications or export notes database for manual review.

## Documentation Naming Convention
- The dropdown filtering functionality of the dashboard/manuals page utilizes the first set of characters up until the first space of a pdf name, known as a "prefix"
  - The prefix is used to filter and display documentations that start with that prefix.
- It is recommended that to utilize this fully, documents would be named #### followed by a space and then whatever else, where #### is the trainer model number.

## Thumbnail Naming Convention
- Thumbnails can be displayed on the dashboard/manuals page in place of a default PDF icon. The naming convention is as follows:
  - The thumbnail will utilize the first set of characters up until the first space, known as the "prefix", and nothing else afterwards.
  - All thumbnails must be in PNG format and be located in the same directory as the documentation. Sizing of the thumbnails may need to be adjusted externally.
  - Example:
    - 1810.png
    - 1820.png

## server.py
- This is the HTTP server that must be running on a desktop/laptop that has access to the Network Drive to read the PDF files from the Network Drive.
- **Configuration:**
  - In the python file, you can change the directory location at line 20 to read from any desired location.
  - Default path: `T:\\APIPO\\DOCTEST`
- **Version 2.0 Changes:**
  - Server URL is now configured in `app/build.gradle` instead of being hardcoded
  - See [Configuration](#configuration) section for details on changing the server URL
  - The app will use the value from `BuildConfig.SERVER_BASE_URL`
  
### Running the Server
```bash
python server.py
```
This will start a server on port 1025 with a GUI for start/stop controls.

## Migration from 1.x to 2.0
### For Developers
1. **Update Server Configuration:**
   - Open `app/build.gradle`
   - Update `SERVER_BASE_URL` with your server IP address
   - Example: `buildConfigField "String", "SERVER_BASE_URL", "\"http://192.168.1.100:1025\""`

2. **Network Security:**
   - If using a new server IP, add it to `app/src/main/res/xml/network_security_config.xml`

3. **Teams Integration:**
   - Remove any dependencies on Teams notifications
   - Issue notes are now logged locally
   - Monitor application logs for issue notifications

### For End Users
- **No breaking changes** - The app UI and functionality remain the same
- **Expanded compatibility** - Now works on Android 8.0+ devices (previously Android 14 only)
- **Teams notifications** - Temporarily unavailable, will return in v2.1+

## Credits
- **Developed by:**
  - Andrew T. Pipo (2023 - 2024)
- **Version 2.0 Updates:**
  - GitHub Copilot AI Agent (2026)
