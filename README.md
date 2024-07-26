# Production Management Application

## Overview
- Version: 1.11.0
- The Production Management Application is a software developed to allow teams to view documentation and record notes with ease.

## Table of Contents
1. [Device Requirements](#device-requirements)
2. [Adobe Acrobat Reader](#adobe-acrobat-reader--required-)
3. [Upload/Update App](#uploadupdate-app)
4. [Frontend Layout](#frontend-layout)
5. [Backend Layout](#backend-layout)
6. [TeamsAPI/Workflows](#teamsapiworkflow)
7. [Documentation Naming Convention](#documentation-naming-convention)
8. [Thumbnail Naming Convention](#documentation-naming-convention)
9. [HTTP Server](#serverpy)
10. [Credits](#credits)

## Device Requirements
- Android OS Version:
  - Android 14 or newer
- Permissions:
  - Permission to use device camera
  - Permission to access storage
  - Permission to access Adobe Acrobat Reader installed on device (see below)
  - Permission to access internet via wifi or internal network
  - Permission to access Microsoft Teams
- Device Settings (Optional):
  - Orientation/Auto-rotation preferable to be locked
  - Taskbar settings adjusted preferably to be minimal buttons only

## Adobe Acrobat Reader (REQUIRED)
- Prior to launching the app for the first time, Adobe Acrobat must be installed on the device.
  - Search for "Adobe PDF" in the Google Play Store
  - Locate "Adobe Acrobat Reader: Edit PDF" and install the app
  - Launch the app to bypass login and to ensure the application is working as intended
- On first launch of the ATech Training Production Management App, and on first click of any document to open:
  - Select the Adobe Acrobat App when the popup appears and select "Always" to ensure Adobe launches everytime
  - Within Adobe, clicking on the triple dots -> pages -> contents will access bookmarks within Adobe

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
- If a note/record is raised as an "issue", a Microsoft Teams workflow is automatically called immediately.
  - Upon a user saving a record that had the "issue" toggled on, the application will create a JSON formatted message that is sent to Microsoft Teams
  - Upon receiving the message, Teams will display the message.

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
- This the http server that must be running on a desktop/laptop that has access to the Network Drive to read the PDF files from the Network Drive until an alternative solution is developed.
- In the python file, you can change the directory location at line 18 to read from any desired location.
- Depending on the computer that is running the HTTP server, you will need to change every instance of ```val url = URL("http://10.2.23.xxx:1025/list")``` to the correct IP address of the host computer with port 1025

## Credits
- Developed by:
  - Andrew T. Pipo (2023 - 2024)
