<template>
    <div v-loading="loading" class="app-container home">
        <!-- 平台统计卡片 -->
        <el-row :gutter="16">
            <el-col :xs="12" :sm="12" :md="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-body">
                        <el-icon :size="42" color="#409eff"><User /></el-icon>
                        <div>
                            <div class="stat-value">{{ userCount }}</div>
                            <div class="stat-label">用户总数</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :xs="12" :sm="12" :md="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-body">
                        <el-icon :size="42" color="#67c23a"><UserFilled /></el-icon>
                        <div>
                            <div class="stat-value">{{ roleCount }}</div>
                            <div class="stat-label">角色总数</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :xs="12" :sm="12" :md="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-body">
                        <el-icon :size="42" color="#e6a23c"><Monitor /></el-icon>
                        <div>
                            <div class="stat-value">{{ onlineCount }}</div>
                            <div class="stat-label">当前在线</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :xs="12" :sm="12" :md="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-body">
                        <el-icon :size="42" color="#f56c6c"><Timer /></el-icon>
                        <div>
                            <div class="stat-value">{{ jobCount }}</div>
                            <div class="stat-label">定时任务</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 快捷入口（按权限显隐） -->
        <el-row :gutter="16" class="chart-row">
            <el-col :span="24">
                <el-card shadow="never">
                    <template #header>
                        <el-icon class="card-icon"><Link /></el-icon>
                        <span>快捷入口</span>
                    </template>
                    <div class="quick-links">
                        <el-button v-hasPermi="['system:user:list']" plain type="primary" @click="go('/system/user')">
                            <el-icon><User /></el-icon>&nbsp;用户管理
                        </el-button>
                        <el-button v-hasPermi="['system:role:list']" plain type="success" @click="go('/system/role')">
                            <el-icon><UserFilled /></el-icon>&nbsp;角色管理
                        </el-button>
                        <el-button v-hasPermi="['monitor:online:list']" plain type="warning" @click="go('/monitor/online')">
                            <el-icon><Monitor /></el-icon>&nbsp;在线用户
                        </el-button>
                        <el-button v-hasPermi="['monitor:job:list']" plain type="danger" @click="go('/monitor/job')">
                            <el-icon><Timer /></el-icon>&nbsp;定时任务
                        </el-button>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts" name="Index">
import { useRouter } from 'vue-router'
import { countUsers, countRoles, countOnline, countJobs } from '@/api/dashboard'

/**
 * 首页平台统计：数据全部走网关静态路由可达的服务（/system/**、/job/**，见 api/dashboard.ts），
 * silent 静默加载——账号缺对应查询权限或服务不可用时显示“–”，不弹全局错误、不阻塞首页。
 * 审计域统计（登录趋势/操作分布/活跃用户）是等保可选能力、仅审计管理员可见，
 * 已迁移至 views/audit/stats（审计中心-审计分析菜单），待网关 audit 路由接通后自动出数。
 */
const router = useRouter()
const loading = ref(false)

const userCount = ref<number | string>('–')
const roleCount = ref<number | string>('–')
const onlineCount = ref<number | string>('–')
const jobCount = ref<number | string>('–')

function totalOf(result: PromiseSettledResult<{ total?: number }>): number | string {
  return result.status === 'fulfilled' ? (result.value?.total ?? '–') : '–'
}

async function loadStatCards() {
  const results = await Promise.allSettled([countUsers(), countRoles(), countOnline(), countJobs()])
  userCount.value = totalOf(results[0])
  roleCount.value = totalOf(results[1])
  onlineCount.value = totalOf(results[2])
  jobCount.value = totalOf(results[3])
}

function go(path: string) {
  router.push(path)
}

onMounted(async () => {
  loading.value = true
  try {
    await loadStatCards()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.home {
    .stat-card {
        margin-bottom: 16px;

        .stat-body {
            display: flex;
            align-items: center;
            gap: 16px;

            .stat-value {
                font-size: 28px;
                font-weight: 600;
                line-height: 1.2;
            }

            .stat-label {
                margin-top: 4px;
                color: #909399;
                font-size: 13px;
            }
        }
    }

    .chart-row {
        margin-bottom: 16px;
    }

    .card-icon {
        width: 1em;
        height: 1em;
        margin-right: 4px;
        vertical-align: middle;
    }

    .quick-links {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
    }
}
</style>
