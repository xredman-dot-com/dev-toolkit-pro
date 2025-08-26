# FastAPI复杂路由示例 - 新策略应该能正确处理的场景
from fastapi import FastAPI
from fastapi.routing import APIRouter

# 主应用
app = FastAPI()

# 用户相关路由器（带前缀）
user_router = APIRouter(prefix="/users")

# API版本路由器
api_v1 = APIRouter(prefix="/api/v1")
api_v2 = APIRouter(prefix="/api/v2")

# 认证路由器
auth_router = APIRouter()

# === 用户路由 ===
@user_router.get("/")
async def list_users():
    """获取用户列表 - 应该解析为: GET /users/"""
    pass

@user_router.get("/{user_id}")
async def get_user(user_id: int):
    """获取单个用户 - 应该解析为: GET /users/{user_id}"""
    pass

@user_router.post("/")
async def create_user():
    """创建用户 - 应该解析为: POST /users/"""
    pass

# === 认证路由 ===
@auth_router.post("/login")
async def login():
    """用户登录 - 应该解析为: POST /api/v1/auth/login"""
    pass

@auth_router.post("/logout")
async def logout():
    """用户登出 - 应该解析为: POST /api/v1/auth/logout"""
    pass

@auth_router.get("/profile")
async def get_profile():
    """获取用户资料 - 应该解析为: GET /api/v1/auth/profile"""
    pass

# === 主应用路由 ===
@app.get("/")
async def root():
    """根路径 - 应该解析为: GET /"""
    pass

@app.get("/health")
async def health_check():
    """健康检查 - 应该解析为: GET /health"""
    pass

# === 复杂的include关系 ===
# 1. 直接include用户路由器到主应用（用户路由器本身有prefix="/users"）
app.include_router(user_router)

# 2. 认证路由器include到API v1（带前缀）
api_v1.include_router(auth_router, prefix="/auth")

# 3. API v1路由器include到主应用
app.include_router(api_v1)

# 4. API v2路由器（空的，但演示结构）
app.include_router(api_v2)

# === 期望的最终路由解析结果 ===
# GET / (root)
# GET /health (health_check)
# GET /users/ (list_users)
# GET /users/{user_id} (get_user)
# POST /users/ (create_user)
# POST /api/v1/auth/login (login)
# POST /api/v1/auth/logout (logout)
# GET /api/v1/auth/profile (get_profile)

# 新的FastAPI策略应该能正确：
# 1. 识别app和各个router的定义
# 2. 收集每个router上的路由装饰器
# 3. 分析include_router关系和前缀
# 4. 构建路由器依赖树
# 5. 正确计算每个端点的完整路径
# 6. 避免重复生成端点