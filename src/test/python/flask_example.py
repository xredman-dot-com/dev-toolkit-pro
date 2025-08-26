from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/')
def hello_world():
    return 'Hello, World!'

@app.route('/users', methods=['GET'])
def get_users():
    return jsonify({"users": []})

@app.route('/users', methods=['POST'])
def create_user():
    return jsonify({"message": "User created"})

@app.route('/users/<int:user_id>', methods=['GET'])
def get_user(user_id):
    return jsonify({"user_id": user_id})

@app.route('/users/<int:user_id>', methods=['PUT'])
def update_user(user_id):
    return jsonify({"message": "User updated", "user_id": user_id})

@app.route('/users/<int:user_id>', methods=['DELETE'])
def delete_user(user_id):
    return jsonify({"message": "User deleted", "user_id": user_id})

if __name__ == '__main__':
    app.run(debug=True)