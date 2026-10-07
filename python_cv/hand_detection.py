import cv2
import mediapipe as mp


# --------------------------------------------------
# MediaPipe classes
# --------------------------------------------------

BaseOptions = mp.tasks.BaseOptions
HandLandmarker = mp.tasks.vision.HandLandmarker
HandLandmarkerOptions = mp.tasks.vision.HandLandmarkerOptions
VisionRunningMode = mp.tasks.vision.RunningMode


# --------------------------------------------------
# Configure MediaPipe
# --------------------------------------------------

options = HandLandmarkerOptions(
    base_options=BaseOptions(
        model_asset_path="hand_landmarker.task"
    ),
    running_mode=VisionRunningMode.VIDEO,
    num_hands=1
)


# Create hand detector
landmarker = HandLandmarker.create_from_options(options)


# --------------------------------------------------
# Open camera
# --------------------------------------------------

camera = cv2.VideoCapture(0)

if not camera.isOpened():
    print("Could not open camera")
    exit()


# --------------------------------------------------
# Variables
# --------------------------------------------------

timestamp = 0

previous_y = None
start_y = None

# Minimum movement between two frames
# required to consider movement significant
movement_threshold = 10

# Movement smaller than this is considered stopped
stop_threshold = 3

# Number of stopped frames required
# to finish a gesture
required_stopped_frames = 5

# Minimum distance required for a real swipe
minimum_gesture_distance = 80

stopped_frames = 0

gesture_active = False

gesture_direction = None

# Store the farthest point reached
peak_y = None


# --------------------------------------------------
# Main camera loop
# --------------------------------------------------

while True:

    success, frame = camera.read()

    if not success:
        print("Could not read frame")
        break


    # --------------------------------------------------
    # OpenCV BGR → RGB
    # --------------------------------------------------

    rgb_frame = cv2.cvtColor(
        frame,
        cv2.COLOR_BGR2RGB
    )


    # --------------------------------------------------
    # Convert to MediaPipe image
    # --------------------------------------------------

    mp_image = mp.Image(
        image_format=mp.ImageFormat.SRGB,
        data=rgb_frame
    )


    # --------------------------------------------------
    # Detect hand
    # --------------------------------------------------

    result = landmarker.detect_for_video(
        mp_image,
        timestamp
    )

    timestamp += 1


    # --------------------------------------------------
    # Find index fingertip
    # --------------------------------------------------

    if result.hand_landmarks:

        hand = result.hand_landmarks[0]

        # Landmark 8 = index fingertip
        index_tip = hand[8]


        # --------------------------------------------------
        # Convert normalized coordinates to pixels
        # --------------------------------------------------

        index_x = int(
            index_tip.x * frame.shape[1]
        )

        index_y = int(
            index_tip.y * frame.shape[0]
        )


        # --------------------------------------------------
        # Frame-to-frame movement
        # --------------------------------------------------

        if previous_y is not None:

            movement = index_y - previous_y


            # --------------------------------------------------
            # Detect if finger is approximately stopped
            # --------------------------------------------------

            if abs(movement) <= stop_threshold:

                stopped_frames += 1

                # ------------------------------------------
                # Gesture finished
                # ------------------------------------------

                if (
                    stopped_frames >= required_stopped_frames
                    and gesture_active
                ):

                    print()
                    print("GESTURE FINISHED")

                    # --------------------------------------
                    # Calculate actual peak distance
                    # --------------------------------------

                    if gesture_direction == "UP":

                        gesture_distance = start_y - peak_y

                    else:

                        gesture_distance = peak_y - start_y


                    print(
                        "Gesture direction:",
                        gesture_direction
                    )

                    print(
                        "Gesture distance:",
                        gesture_distance
                    )


                    # --------------------------------------
                    # Check minimum gesture distance
                    # --------------------------------------

                    if gesture_distance >= minimum_gesture_distance:

                        # ----------------------------------
                        # UP
                        # ----------------------------------

                        if gesture_direction == "UP":

                            print("UP GESTURE")
                            print("NEXT REEL")


                        # ----------------------------------
                        # DOWN
                        # ----------------------------------

                        elif gesture_direction == "DOWN":

                            print("DOWN GESTURE")
                            print("PREVIOUS REEL")


                    else:

                        print("GESTURE TOO SMALL")
                        print("NO ACTION")


                    # --------------------------------------
                    # Reset gesture
                    # --------------------------------------

                    gesture_active = False
                    start_y = None
                    peak_y = None
                    gesture_direction = None
                    stopped_frames = 0


            else:

                # Finger started moving again
                stopped_frames = 0


            # --------------------------------------------------
            # Detect gesture start
            # --------------------------------------------------

            if abs(movement) > movement_threshold:

                # ------------------------------------------
                # Start a new gesture
                # ------------------------------------------

                if not gesture_active:

                    gesture_active = True

                    # Gesture starts from previous position
                    start_y = previous_y

                    # First significant movement determines
                    # the gesture direction
                    if movement < 0:

                        gesture_direction = "UP"

                    else:

                        gesture_direction = "DOWN"


                    # Initial peak
                    peak_y = previous_y


                    print()
                    print("GESTURE STARTED")

                    print(
                        "Gesture Start Y:",
                        start_y
                    )

                    print(
                        "Gesture Direction:",
                        gesture_direction
                    )


                # --------------------------------------------------
                # Update peak position
                # --------------------------------------------------

                if gesture_direction == "UP":

                    # Smaller Y means higher on screen
                    if index_y < peak_y:

                        peak_y = index_y


                elif gesture_direction == "DOWN":

                    # Larger Y means lower on screen
                    if index_y > peak_y:

                        peak_y = index_y


                # --------------------------------------------------
                # Check direction consistency
                # --------------------------------------------------

                if movement < 0:

                    if gesture_direction == "UP":

                        print("UP movement")

                    else:

                        print("Opposite movement detected")

                else:

                    if gesture_direction == "DOWN":

                        print("DOWN movement")

                    else:

                        print("Opposite movement detected")


        # --------------------------------------------------
        # Display gesture information
        # --------------------------------------------------

        if gesture_active:

            if gesture_direction == "UP":

                current_distance = start_y - index_y

            else:

                current_distance = index_y - start_y


            print(
                "Start Y:",
                start_y,
                "Current Y:",
                index_y,
                "Peak Y:",
                peak_y,
                "Distance:",
                current_distance
            )


        # --------------------------------------------------
        # Save current Y
        # --------------------------------------------------

        previous_y = index_y


        # --------------------------------------------------
        # Print fingertip position
        # --------------------------------------------------

        print(
            "Index fingertip:",
            index_x,
            index_y
        )


        # --------------------------------------------------
        # Draw red dot
        # --------------------------------------------------

        cv2.circle(
            frame,
            (index_x, index_y),
            10,
            (0, 0, 255),
            -1
        )


    # --------------------------------------------------
    # Show camera
    # --------------------------------------------------

    cv2.imshow(
        "Index Fingertip",
        frame
    )


    # --------------------------------------------------
    # Press Q to quit
    # --------------------------------------------------

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break


# --------------------------------------------------
# Cleanup
# --------------------------------------------------

camera.release()

cv2.destroyAllWindows()

landmarker.close()



# | ID | Landmark |
# |---:|---|
# | 0 | Wrist |
# | 1 | Thumb CMC |
# | 2 | Thumb MCP |
# | 3 | Thumb IP |
# | 4 | Thumb tip |
# | 5 | Index MCP |
# | 6 | Index PIP |
# | 7 | Index DIP |
# | 8 | Index tip |
# | 9 | Middle MCP |
# | 10 | Middle PIP |
# | 11 | Middle DIP |
# | 12 | Middle tip |
# | 13 | Ring MCP |
# | 14 | Ring PIP |
# | 15 | Ring DIP |
# | 16 | Ring tip |
# | 17 | Pinky MCP |
# | 18 | Pinky PIP |
# | 19 | Pinky DIP |
# | 20 | Pinky tip |





# Start Y = 298

# Frame       Current Y       Total Movement
# -------------------------------------------
# 1              298                0
# 2              296               -2
# 3              274              -24
# 4              258              -40
# 5              250              -48
# 6              248              -50
# 7              243              -55
# 8              240              -58


# Finger starts
#      ↓
# 298
# 274
# 250
# 220
# 190
#      ↓
# 190
# 190
# 190
#      ↓
# STOP


# Previous    Current    Movement    abs()
# -----------------------------------------
# 298         298           0          0  ← stopped
# 298         296          -2          2  ← stopped
# 296         297           1          1  ← stopped
# 297         299           2          2  ← stopped

# 299         280         -19         19  ← moving
# 280         250         -30         30  ← moving