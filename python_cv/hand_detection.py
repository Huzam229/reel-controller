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

movement_threshold = 10

stop_threshold = 3
required_stopped_frames = 5

stopped_frames = 0

gesture_active = False


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

        # Get first detected hand
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
        # Set starting Y position
        # --------------------------------------------------

        if start_y is None:

            start_y = index_y

            print(
                "Start Y:",
                start_y
            )


        # --------------------------------------------------
        # Calculate total movement
        # --------------------------------------------------

        total_movement = index_y - start_y


        print(
            "Start Y:",
            start_y,
            "Current Y:",
            index_y,
            "Total movement:",
            total_movement
        )


        # --------------------------------------------------
        # Frame-to-frame movement
        # --------------------------------------------------

        if previous_y is not None:

            movement = index_y - previous_y


            # --------------------------------------------------
            # Check if finger is approximately stopped
            # --------------------------------------------------

            if abs(movement) <= stop_threshold:

                stopped_frames += 1


                if stopped_frames >= required_stopped_frames:

                    print(
                        "Finger is stopped"
                    )


            else:

                stopped_frames = 0


            # --------------------------------------------------
            # Detect significant movement
            # --------------------------------------------------

            if movement < -movement_threshold:

                print(
                    "Significant UP movement"
                )


            elif movement > movement_threshold:

                print(
                    "Significant DOWN movement"
                )


        # --------------------------------------------------
        # Save current Y for next frame
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
        # Draw red dot on index fingertip
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