from flask import Flask, jsonify, request

app = Flask(__name__)


# Store the latest detected gesture
latest_gesture = None


# --------------------------------------------------
# GET gesture
# Flutter uses this
# --------------------------------------------------

@app.route("/gesture", methods=["GET"])
def get_gesture():

    global latest_gesture

    gesture = latest_gesture

    # Clear the gesture after sending it
    latest_gesture = None

    return jsonify({
        "gesture": gesture
    })


# --------------------------------------------------
# POST gesture
# Python gesture detector uses this
# --------------------------------------------------

@app.route("/gesture", methods=["POST"])
def set_gesture():

    global latest_gesture

    data = request.get_json()

    if not data or "gesture" not in data:

        return jsonify({
            "error": "gesture is required"
        }), 400

    latest_gesture = data["gesture"]

    print("Gesture received:", latest_gesture)

    return jsonify({
        "gesture": latest_gesture
    })


# --------------------------------------------------
# Start server
# --------------------------------------------------

if __name__ == "__main__":

    app.run(
        host="0.0.0.0",
        port=5000
    )