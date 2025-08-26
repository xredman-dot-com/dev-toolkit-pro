from fastapi import FastAPI, APIRouter, Depends
from fastapi.responses import JSONResponse
from typing import Optional

# 创建主应用
app = FastAPI(title="Complex FastAPI Example", version="1.0.0")

# 创建多个路由器
user_router = APIRouter()
api_router = APIRouter()
auth_router = APIRouter()
product_router = APIRouter()

# 主应用的直接路由
@app.get("/")
def read_root():
    return {"message": "Welcome to FastAPI"}

@app.get("/health")
def health_check():
    return {"status": "healthy"}

@app.post("/webhook")
def receive_webhook(payload: dict):
    return {"message": "Webhook received"}

# 用户路由器
@user_router.get("/")
def get_all_users():
    return {"users": []}

@user_router.get("/{user_id}")
def get_user(user_id: int):
    return {"user_id": user_id}

@user_router.post("/")
def create_user(user: dict):
    return {"message": "User created", "user": user}

@user_router.put("/{user_id}")
def update_user(user_id: int, user: dict):
    return {"message": "User updated", "user_id": user_id}

@user_router.delete("/{user_id}")
def delete_user(user_id: int):
    return {"message": "User deleted", "user_id": user_id}

@user_router.patch("/{user_id}/status")
def update_user_status(user_id: int, status: str):
    return {"user_id": user_id, "status": status}

# 认证路由器
@auth_router.post("/login")
def login(credentials: dict):
    return {"token": "fake-jwt-token"}

@auth_router.post("/logout")
def logout():
    return {"message": "Logged out"}

@auth_router.get("/profile")
def get_profile():
    return {"profile": "user profile"}

# 产品路由器
@product_router.get("/")
def get_products(category: Optional[str] = None):
    return {"products": [], "category": category}

@product_router.get("/{product_id}")
def get_product(product_id: int):
    return {"product_id": product_id}

@product_router.post("/")
def create_product(product: dict):
    return {"message": "Product created"}

@product_router.put("/{product_id}")
def update_product(product_id: int, product: dict):
    return {"message": "Product updated"}

@product_router.delete("/{product_id}")
def delete_product(product_id: int):
    return {"message": "Product deleted"}

# API路由器包含其他路由器
api_router.include_router(user_router, prefix="/users", tags=["users"])
api_router.include_router(auth_router, prefix="/auth", tags=["authentication"])
api_router.include_router(product_router, prefix="/products", tags=["products"])

# 主应用包含API路由器
app.include_router(api_router, prefix="/api/v1")

# 直接包含到主应用的路由器（无前缀）
admin_router = APIRouter()

@admin_router.get("/stats")
def get_admin_stats():
    return {"stats": "admin statistics"}

@admin_router.post("/maintenance")
def toggle_maintenance(enabled: bool):
    return {"maintenance_mode": enabled}

app.include_router(admin_router, prefix="/admin")

# 嵌套包含的复杂示例
v2_router = APIRouter()
v2_user_router = APIRouter()

@v2_user_router.get("/profile")
def get_user_profile_v2():
    return {"version": "2.0", "profile": "enhanced profile"}

@v2_user_router.post("/avatar")
def upload_avatar():
    return {"message": "Avatar uploaded"}

v2_router.include_router(v2_user_router, prefix="/users")
app.include_router(v2_router, prefix="/api/v2")

# 带参数的路由
@app.get("/search")
def search(q: str, limit: int = 10, offset: int = 0):
    return {
        "query": q,
        "limit": limit,
        "offset": offset,
        "results": []
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)