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


timestamp = 0


# --------------------------------------------------
# Hand connections
# --------------------------------------------------

connections = [
    # Thumb
    (0, 1),
    (1, 2),
    (2, 3),
    (3, 4),

    # Index finger
    (0, 5),
    (5, 6),
    (6, 7),
    (7, 8),

    # Middle finger
    (0, 9),
    (9, 10),
    (10, 11),
    (11, 12),

    # Ring finger
    (0, 13),
    (13, 14),
    (14, 15),
    (15, 16),

    # Pinky
    (0, 17),
    (17, 18),
    (18, 19),
    (19, 20),

    # Palm
    (5, 9),
    (9, 13),
    (13, 17)
]


# --------------------------------------------------
# Main camera loop
# --------------------------------------------------

while True:

    success, frame = camera.read()

    if not success:
        print("Could not read frame")
        break


    # OpenCV BGR → RGB
    rgb_frame = cv2.cvtColor(
        frame,
        cv2.COLOR_BGR2RGB
    )


    # Convert to MediaPipe image
    mp_image = mp.Image(
        image_format=mp.ImageFormat.SRGB,
        data=rgb_frame
    )


    # Detect hand
    result = landmarker.detect_for_video(
        mp_image,
        timestamp
    )

    timestamp += 1


    # --------------------------------------------------
    # Draw landmarks
    # --------------------------------------------------

    if result.hand_landmarks:

        hand = result.hand_landmarks[0]

        index_tip = hand[8]

        print(
          "Index fingertip:",
          index_tip.x,
          index_tip.y
        )

        

        # Convert normalized coordinates
        # to pixel coordinates
        points = []

        for landmark in hand:

            x = int(
                landmark.x * frame.shape[1]
            )

            y = int(
                landmark.y * frame.shape[0]
            )

            points.append((x, y))


        # Draw connections
        for start, end in connections:

            cv2.line(
                frame,
                points[start],
                points[end],
                (0, 255, 0),
                2
            )


        # Draw landmark points
        for i, (x, y) in enumerate(points):

            cv2.circle(
                frame,
                (x, y),
                5,
                (0, 0, 255),
                -1
            )

            # Draw landmark number
            cv2.putText(
                frame,
                str(i),
                (x + 5, y - 5),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.4,
                (255, 255, 255),
                1
            )


    # Show camera
    cv2.imshow(
        "Hand Landmarks",
        frame
    )


    # Press Q to quit
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