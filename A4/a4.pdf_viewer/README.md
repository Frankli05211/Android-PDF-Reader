## Personal Information
Boyang Li

20847992 b392li

Android SDK: Android API 33

AVD detail: Pixel C API 30

## Source for button images
* Pencil button: https://www.flaticon.com/free-icon/pencil_481874?term=pencil&page=1&position=2&page=1&position=2&related_id=481874&origin=search
* Highlighter button: https://www.flaticon.com/premium-icon/highlighter_3551903?term=highlighter&page=1&position=55&page=1&position=55&related_id=3551903&origin=search
* Eraser button: https://www.flaticon.com/free-icon/eraser_171383?term=eraser&page=1&position=2&page=1&position=2&related_id=171383&origin=search
* cursor button: https://www.flaticon.com/premium-icon/click_1481058?term=cursor&page=1&position=19&page=1&position=19&related_id=1481058&origin=search
* undo button: https://www.flaticon.com/free-icon/undo-circular-arrow_44426?term=undo&page=1&position=1&page=1&position=1&related_id=44426&origin=search
* redo button: https://www.flaticon.com/free-icon/redo-arrow-symbol_44650?term=redo&page=1&position=1&page=1&position=1&related_id=44650&origin=search

## Source for sample code
* Using the pdf_viewer sample code provided in course home
* Retrieve the function to find the closest point from a point to a line from the closestPoint sample code

## Features implemented in the app
1. Erase feature implemented:
* After choosing the erase feature by pressing the erase button, the user can erase any shapes drawn
in the current page by pressing the cursor down at a location close to those shapes
* The erase feature does not support drag, so only push the cursor down and up counts for 1 erase
event

2. Cursor feature implemented:
* When the user chooses the cursor feature by pressing the cursor button, they can either drag the
current pdf image or zoom-in and zoom-out
* The user can either drag the current pdf image by pressing down the cursor and moving
* The user can zoom-in and zoom-out the current pdf image by pressing both the key "Ctrl" and cursor down and moving.

3. Draw and highlight implemented as normal by pressing the pencil or brush buttons

4. Move left, move right and status bar:
* There is a status bar at the middle of the top toolbar, and the moving left and right button around
that status bar named "Prev" and "Next"
* Pressing the "Prev" button will navigate the page forward
* Pressing the "Next" button will navigate the page backwards
* The status bar is placed at the center

5. Undo and redo implemented as normal by pressing the undo or redo buttons
* There is no shortcut for undo or redo

6. Reset button implemented:
* There is a reset button at menu bar to reset the image scale to 1*1 size and position to (0, 0) so
that the user can find the pdf image if it lies out of the view

7. Restart app implemented:
* The user are able to exit and restart the application, the page number and all data will be saved and shown

8. Orientation implemented:
* When changing orientation, the page number and data will be saved, and the app will rescaled the pdf
image to 1*1 size if it is portrait, and to 3*3 size if it is landscape, and set the pdf position
to (0, 0)