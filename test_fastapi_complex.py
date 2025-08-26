# 复杂的FastAPI路由测试场景
from fastapi import FastAPI
from fastapi.routing import APIRouter

# 创建主应用
app = FastAPI()

# 创建多个路由器
user_router = APIRouter(prefix="/users")
admin_router = APIRouter(prefix="/admin")
api_v1_router = APIRouter(prefix="/api/v1")
api_v2_router = APIRouter(prefix="/api/v2")

# 嵌套路由器的情况
auth_router = APIRouter(prefix="/auth")
profile_router = APIRouter()

# 用户相关路由
@user_router.get("/")
async def get_users():
    pass

@user_router.get("/{user_id}")
async def get_user(user_id: int):
    pass

@user_router.post("/")
async def create_user():
    pass

# 管理员路由
@admin_router.get("/dashboard")
async def admin_dashboard():
    pass

@admin_router.get("/users")
async def admin_get_users():
    pass

# 认证路由
@auth_router.post("/login")
async def login():
    pass

@auth_router.post("/logout")
async def logout():
    pass

# 用户资料路由
@profile_router.get("/profile")
async def get_profile():
    pass

@profile_router.put("/profile")
async def update_profile():
    pass

# 复杂的include情况：
# 1. 直接include到app
app.include_router(user_router)
app.include_router(admin_router)

# 2. include到另一个router，然后再include到app
api_v1_router.include_router(auth_router)
api_v1_router.include_router(profile_router, prefix="/user")

# 3. 嵌套include
app.include_router(api_v1_router)
app.include_router(api_v2_router)

# 4. 直接在app上定义的路由
@app.get("/")
async def root():
    pass

@app.get("/health")
async def health_check():
    pass

# 期望的最终路由应该是：
# GET / (root)
# GET /health (health_check)
# GET /users/ (get_users)
# GET /users/{user_id} (get_user)
# POST /users/ (create_user)
# GET /admin/dashboard (admin_dashboard)
# GET /admin/users (admin_get_users)
# GET /api/v1/auth/login (login)
# POST /api/v1/auth/logout (logout)
# GET /api/v1/user/profile (get_profile)
# PUT /api/v1/user/profile (update_profile)