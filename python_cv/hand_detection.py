import cv2
import mediapipe as mp
import requests


# --------------------------------------------------
# MediaPipe classes
# --------------------------------------------------

BaseOptions = mp.tasks.BaseOptions
HandLandmarker = mp.tasks.vision.HandLandmarker
HandLandmarkerOptions = mp.tasks.vision.HandLandmarkerOptions
VisionRunningMode = mp.tasks.vision.RunningMode


FLASK_URL = "http://127.0.0.1:5000/gesture"


def send_gesture(gesture):

    try:

        response = requests.post(
            FLASK_URL,
            json={
                "gesture": gesture
            },
            timeout=1
        )

        print("Sent gesture:", gesture)
        print("Flask response:", response.json())

    except requests.RequestException as e:

        print("Could not send gesture to Flask:", e)


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


# --------------------------------------------------
# Create hand detector
# --------------------------------------------------

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
peak_y = None

gesture_active = False
gesture_direction = None

stopped_frames = 0


# --------------------------------------------------
# Gesture configuration
# --------------------------------------------------

# Minimum movement between two frames
# required to consider movement significant
movement_threshold = 5


# Opposite movement larger than this
# cancels the current gesture
direction_change_threshold = 5


# Movement smaller than this
# is considered stopped
stop_threshold = 3


# Number of stopped frames required
# to finish a gesture
required_stopped_frames = 5


# Minimum total gesture distance
# required for a real swipe
minimum_gesture_distance = 25


# --------------------------------------------------
# Cooldown configuration
# --------------------------------------------------

# Number of frames to ignore after
# a gesture is cancelled
cooldown_duration = 5

cooldown_frames = 0

# Last swipe shown on the camera window
screen_message = ""


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
        # First frame
        # --------------------------------------------------

        if previous_y is None:

            previous_y = index_y

        else:

            # --------------------------------------------------
            # Calculate movement
            # --------------------------------------------------

            movement = index_y - previous_y


            # ==================================================
            # COOLDOWN STATE
            # ==================================================

            if cooldown_frames > 0:

                cooldown_frames -= 1

                # Ignore all gesture processing
                # during cooldown
                gesture_active = False
                start_y = None
                peak_y = None
                gesture_direction = None
                stopped_frames = 0

                print(
                    "COOLDOWN:",
                    cooldown_frames
                )


            # ==================================================
            # NORMAL GESTURE PROCESSING
            # ==================================================

            else:

                # --------------------------------------------------
                # Detect stopped movement
                # --------------------------------------------------

                if abs(movement) <= stop_threshold:

                    stopped_frames += 1

                else:

                    stopped_frames = 0


                # ==================================================
                # START NEW GESTURE
                # ==================================================

                if (
                    not gesture_active
                    and abs(movement) > movement_threshold
                ):

                    gesture_active = True

                    # Gesture starts from previous position
                    start_y = previous_y

                    # Initial peak
                    peak_y = previous_y


                    # --------------------------------------------------
                    # Determine direction
                    # --------------------------------------------------

                    if movement < 0:

                        gesture_direction = "UP"
                        screen_message = "SWIPE UP"

                    else:

                        gesture_direction = "DOWN"
                        screen_message = "SWIPE DOWN"


                    print()
                    print("==============================")
                    print("GESTURE STARTED")
                    print(
                        "Gesture Start Y:",
                        start_y
                    )
                    print(
                        "Gesture Direction:",
                        gesture_direction
                    )
                    print("==============================")


                # ==================================================
                # ACTIVE GESTURE
                # ==================================================

                if gesture_active:

                    # --------------------------------------------------
                    # Update peak
                    # --------------------------------------------------

                    if gesture_direction == "UP":

                        # Smaller Y = higher on screen

                        if index_y < peak_y:

                            peak_y = index_y


                    elif gesture_direction == "DOWN":

                        # Larger Y = lower on screen

                        if index_y > peak_y:

                            peak_y = index_y


                    # --------------------------------------------------
                    # Check direction consistency
                    # --------------------------------------------------

                    if movement < -direction_change_threshold:

                        if gesture_direction == "UP":

                            print("UP movement")

                        else:

                            print("STRONG OPPOSITE MOVEMENT")
                            print("CANCELING GESTURE")
                            screen_message = "SWIPE CANCELED"

                            # Cancel gesture
                            gesture_active = False

                            start_y = None
                            peak_y = None
                            gesture_direction = None
                            stopped_frames = 0

                            # Start cooldown
                            cooldown_frames = cooldown_duration


                    elif movement > direction_change_threshold:

                        if gesture_direction == "DOWN":

                            print("DOWN movement")

                        else:

                            print("STRONG OPPOSITE MOVEMENT")
                            print("CANCELING GESTURE")
                            screen_message = "SWIPE CANCELED"

                            # Cancel gesture
                            gesture_active = False

                            start_y = None
                            peak_y = None
                            gesture_direction = None
                            stopped_frames = 0

                            # Start cooldown
                            cooldown_frames = cooldown_duration


                # ==================================================
                # GESTURE FINISHED
                # ==================================================

                if (
                    gesture_active
                    and stopped_frames >= required_stopped_frames
                ):

                    print()
                    print("==============================")
                    print("GESTURE FINISHED")
                    print("==============================")


                    # --------------------------------------------------
                    # Calculate peak distance
                    # --------------------------------------------------

                    if gesture_direction == "UP":

                        gesture_distance = (
                            start_y - peak_y
                        )

                    else:

                        gesture_distance = (
                            peak_y - start_y
                        )


                    print(
                        "Gesture direction:",
                        gesture_direction
                    )

                    print(
                        "Gesture distance:",
                        gesture_distance
                    )


                    # --------------------------------------------------
                    # Check minimum gesture distance
                    # --------------------------------------------------

                    if (
                        gesture_distance
                        >= minimum_gesture_distance
                    ):

                        # ----------------------------------------------
                        # UP
                        # ----------------------------------------------

                        if gesture_direction == "UP":

                            print("UP GESTURE")
                            print("NEXT REEL")
                            send_gesture("NEXT_REEL")
                            screen_message = "SWIPE UP - NEXT REEL"


                        # ----------------------------------------------
                        # DOWN
                        # ----------------------------------------------

                        elif gesture_direction == "DOWN":

                            print("DOWN GESTURE")
                            print("PREVIOUS REEL")
                            send_gesture("PREVIOUS_REEL")
                            screen_message = "SWIPE DOWN - PREVIOUS REEL"


                    else:

                        print("GESTURE TOO SMALL")
                        print("NO ACTION")
                        screen_message = "SWIPE TOO SMALL"


                    # --------------------------------------------------
                    # Reset gesture
                    # --------------------------------------------------

                    gesture_active = False

                    start_y = None
                    peak_y = None
                    gesture_direction = None

                    stopped_frames = 0


                # --------------------------------------------------
                # Save current Y
                # --------------------------------------------------

                previous_y = index_y


        # ==================================================
        # Display gesture information
        # ==================================================

        if gesture_active:

            if gesture_direction == "UP":

                current_distance = (
                    start_y - index_y
                )

            else:

                current_distance = (
                    index_y - start_y
                )


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
        # Print fingertip position
        # --------------------------------------------------

        print(
            "Index fingertip:",
            index_x,
            index_y
        )


        # --------------------------------------------------
        # Draw fingertip
        # --------------------------------------------------

        cv2.circle(
            frame,
            (index_x, index_y),
            10,
            (0, 0, 255),
            -1
        )


        # --------------------------------------------------
        # Display cooldown on camera
        # --------------------------------------------------

        if cooldown_frames > 0:

            cv2.putText(
                frame,
                "COOLDOWN",
                (30, 50),
                cv2.FONT_HERSHEY_SIMPLEX,
                1,
                (0, 0, 255),
                2
            )


        # --------------------------------------------------
        # Display current gesture
        # --------------------------------------------------

        if gesture_active:

            cv2.putText(
                frame,
                gesture_direction,
                (30, 100),
                cv2.FONT_HERSHEY_SIMPLEX,
                1,
                (0, 255, 0),
                2
            )


    # --------------------------------------------------
    # Show camera
    # --------------------------------------------------

    if screen_message:

        if "NEXT" in screen_message or screen_message == "SWIPE UP":

            message_color = (0, 255, 0)

        elif "PREVIOUS" in screen_message or screen_message == "SWIPE DOWN":

            message_color = (0, 0, 255)

        else:

            message_color = (0, 255, 255)

        cv2.putText(
            frame,
            screen_message,
            (30, 160),
            cv2.FONT_HERSHEY_SIMPLEX,
            1,
            message_color,
            2
        )

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