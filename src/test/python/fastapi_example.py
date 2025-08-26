from fastapi import FastAPI, APIRouter
from fastapi.responses import JSONResponse

app = FastAPI()
router = APIRouter()

@app.get("/")
def read_root():
    return {"Hello": "World"}

@app.get("/users/{user_id}")
def read_user(user_id: int):
    return {"user_id": user_id}

@app.post("/users")
def create_user(user: dict):
    return {"message": "User created", "user": user}

@app.put("/users/{user_id}")
def update_user(user_id: int, user: dict):
    return {"message": "User updated", "user_id": user_id, "user": user}

@app.delete("/users/{user_id}")
def delete_user(user_id: int):
    return {"message": "User deleted", "user_id": user_id}

# 使用路由器
@router.get("/items/{item_id}")
def read_item(item_id: int):
    return {"item_id": item_id}

@router.post("/items")
def create_item(item: dict):
    return {"message": "Item created", "item": item}

app.include_router(router, prefix="/api/v1")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)