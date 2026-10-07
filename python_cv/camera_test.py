import cv2

camera  = cv2.VideoCapture(0)

if not camera.isOpened():
    print("Could not open camera")
    exit()


while True:
    success, frame = camera.read()

    if not success:
        print("Could not read frame")
        break

    cv2.imshow("Reel Controller - Python Camera Test", frame)

    key = cv2.waitKey(1)

    if key == ord("q"):
        break


camera.release()
cv2.destroyAllWindows()