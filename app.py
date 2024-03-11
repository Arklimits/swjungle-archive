from flask import Flask, render_template, jsonify, request
app = Flask(__name__)

from pymongo import MongoClient
client = MongoClient('mongodb+srv://test:sparta@cluster0.wogctrp.mongodb.net/?retryWrites=true&w=majority')
db = client.dbjungle

@app.route('/')
def home():
        return render_template('index.html')


@app.route('/api/get', methods=['GET'])
def readTodo():
    result = list(db.todos.find({}, {'_id': 0}))

    return jsonify({'result': 'success', 'todos': result})


@app.route('/edit_todo', methods=['GET'])
def updateTodo():
    org_todo = request.args.get('org')
    upd_todo = request.args.get('edit')

    selector = {'todo': org_todo}

    edit = {"$set": {'todo': upd_todo}}
    db.todos.update_one(selector, edit)

    return render_template('index.html')


@app.route('/api/del', methods=['DELETE'])
def deleteTodo():
    del_todo = request.form['del_todo']
    del_done = int(request.form['del_done'])

    selector = {'todo': del_todo, 'done': del_done}

    db.todos.delete_one(selector)

    return jsonify({'result': 'success'})


@app.route('/api/post', methods=['POST'])
def makeTodo():
    input_todo = request.form['input_todo']

    todo = {'todo': input_todo, 'done': 0}

    db.todos.insert_one(todo)

    return jsonify({'result': 'success'})


@app.route('/api/comp', methods=['PUT'])
def completeTodo():
    done_todo = request.form['done_todo']

    selector = {'todo': done_todo, 'done': 0}
    edit = {"$set": {'todo': done_todo, 'done': 1}}

    db.todos.update_one(selector, edit)

    return jsonify({'result': 'success'})


if __name__ == '__main__':
   app.run('0.0.0.0',port=5000,debug=True)