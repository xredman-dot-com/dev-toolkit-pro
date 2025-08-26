# FastAPI增强功能测试示例
from fastapi import FastAPI, APIRouter, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from pydantic import BaseModel
from typing import List, Optional
import logging

# Pydantic模型定义
class UserBase(BaseModel):
    username: str
    email: str
    is_active: bool = True

class UserCreate(UserBase):
    password: str

class UserResponse(UserBase):
    id: int
    created_at: str

class ProductModel(BaseModel):
    name: str
    price: float
    description: Optional[str] = None

# 依赖注入函数
def get_current_user():
    """获取当前用户的依赖函数"""
    return {"user_id": 123, "username": "test_user"}

def get_db_session():
    """获取数据库会话的依赖函数"""
    return "mock_db_session"

# 创建主应用
app = FastAPI(
    title="Enhanced FastAPI Demo",
    description="演示增强策略功能的FastAPI应用",
    version="1.0.0"
)

# 添加中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=["example.com", "*.example.com"]
)

# 创建路由器
users_router = APIRouter(
    prefix="/users",
    tags=["users"],
    dependencies=[Depends(get_db_session)]
)

products_router = APIRouter(prefix="/products")
admin_router = APIRouter(prefix="/admin", tags=["admin"])

# === 用户路由（带完整元数据） ===

@users_router.get(
    "/",
    summary="获取用户列表",
    description="返回系统中所有用户的列表，支持分页",
    response_model=List[UserResponse],
    tags=["users", "list"]
)
async def list_users(
    skip: int = 0,
    limit: int = 100,
    current_user=Depends(get_current_user)
):
    """获取用户列表的端点函数"""
    pass

@users_router.get(
    "/{user_id}",
    summary="获取单个用户",
    description="根据用户ID获取用户详细信息",
    response_model=UserResponse,
    tags=["users", "detail"]
)
async def get_user(
    user_id: int,
    db_session=Depends(get_db_session)
):
    """获取单个用户的端点函数"""
    pass

@users_router.post(
    "/",
    summary="创建新用户",
    description="创建一个新的用户账户",
    response_model=UserResponse,
    tags=["users", "create"],
    status_code=201
)
async def create_user(
    user: UserCreate,
    db_session=Depends(get_db_session)
):
    """创建用户的端点函数"""
    pass

@users_router.delete(
    "/{user_id}",
    summary="删除用户",
    description="删除指定ID的用户（已废弃，请使用软删除）",
    tags=["users", "delete"],
    deprecated=True
)
async def delete_user(
    user_id: int,
    current_user=Depends(get_current_user),
    db_session=Depends(get_db_session)
):
    """删除用户的端点函数（已废弃）"""
    pass

# === 产品路由 ===

@products_router.get(
    "/",
    summary="获取产品列表",
    response_model=List[ProductModel],
    tags=["products"]
)
async def list_products():
    """获取产品列表"""
    pass

@products_router.post(
    "/",
    summary="创建产品",
    response_model=ProductModel,
    tags=["products", "create"]
)
async def create_product(
    product: ProductModel,
    current_user=Depends(get_current_user)
):
    """创建新产品"""
    pass

# === 管理员路由 ===

@admin_router.get(
    "/dashboard",
    summary="管理员仪表板",
    description="管理员专用的仪表板页面",
    tags=["admin", "dashboard"]
)
async def admin_dashboard(
    current_user=Depends(get_current_user)
):
    """管理员仪表板"""
    pass

@admin_router.get(
    "/stats",
    summary="系统统计",
    description="获取系统的各种统计信息",
    tags=["admin", "statistics"]
)
async def get_system_stats(
    current_user=Depends(get_current_user),
    db_session=Depends(get_db_session)
):
    """获取系统统计信息"""
    pass

# === 主应用路由 ===

@app.get(
    "/",
    summary="根路径",
    description="API的根路径，返回欢迎信息",
    tags=["root"]
)
async def root():
    """根路径端点"""
    return {"message": "Welcome to Enhanced FastAPI Demo"}

@app.get(
    "/health",
    summary="健康检查",
    description="检查API服务的健康状态",
    tags=["health", "monitoring"]
)
async def health_check():
    """健康检查端点"""
    return {"status": "healthy", "version": "1.0.0"}

# === 复杂的include关系 ===

# 创建API版本路由器
api_v1 = APIRouter(prefix="/api/v1", tags=["v1"])

# 将用户和产品路由器包含到v1版本中
api_v1.include_router(users_router)
api_v1.include_router(products_router, tags=["v1-products"])

# 将管理员路由器直接包含到主应用
app.include_router(admin_router)

# 将v1版本包含到主应用
app.include_router(api_v1)

# 期望的增强解析结果应该包含：
# GET / - 根路径 [root]
# GET /health - 健康检查 [health, monitoring]
# GET /admin/dashboard - 管理员仪表板 [admin, dashboard] (deps: get_current_user)
# GET /admin/stats - 系统统计 [admin, statistics] (deps: get_current_user, get_db_session)
# GET /api/v1/users/ - 获取用户列表 [users, list] -> List[UserResponse] (deps: get_current_user)
# GET /api/v1/users/{user_id} - 获取单个用户 [users, detail] -> UserResponse (deps: get_db_session)
# POST /api/v1/users/ - 创建新用户 [users, create] -> UserResponse (deps: get_db_session)
# DELETE /api/v1/users/{user_id} - 删除用户 [users, delete] [DEPRECATED] (deps: get_current_user, get_db_session)
# GET /api/v1/products/ - 获取产品列表 [products] -> List[ProductModel]
# POST /api/v1/products/ - 创建产品 [products, create] -> ProductModel (deps: get_current_user)