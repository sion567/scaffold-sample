<template>
    <div v-loading="loading" class="app-container home">
        <!-- 统计卡片 -->
        <el-row :gutter="16">
            <el-col :xs="12" :sm="12" :md="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-body">
                        <el-icon :size="42" color="#409eff"><User /></el-icon>
                        <div>
                            <div class="stat-value">{{ onlineCount }}</div>
                            <div class="stat-label">在线用户</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :xs="12" :sm="12" :md="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-body">
                        <el-icon :size="42" color="#67c23a"><TrendCharts /></el-icon>
                        <div>
                            <div class="stat-value">{{ todayLoginCount }}</div>
                            <div class="stat-label">今日登录</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :xs="12" :sm="12" :md="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-body">
                        <el-icon :size="42" color="#e6a23c"><Odometer /></el-icon>
                        <div>
                            <div class="stat-value">{{ todayOperCount }}</div>
                            <div class="stat-label">今日操作</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :xs="12" :sm="12" :md="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-body">
                        <el-icon :size="42" color="#f56c6c"><WarningFilled /></el-icon>
                        <div>
                            <div class="stat-value">{{ todayLoginFailCount }}</div>
                            <div class="stat-label">今日登录失败</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 登录趋势 / 操作类型分布 -->
        <el-row :gutter="16" class="chart-row">
            <el-col :xs="24" :lg="14">
                <el-card shadow="never">
                    <template #header>
                        <el-icon class="card-icon"><TrendCharts /></el-icon>
                        <span>近7日登录趋势</span>
                    </template>
                    <div ref="loginTrendRef" class="chart chart-lg" />
                </el-card>
            </el-col>
            <el-col :xs="24" :lg="10">
                <el-card shadow="never">
                    <template #header>
                        <el-icon class="card-icon"><PieChart /></el-icon>
                        <span>操作类型分布（近7日）</span>
                    </template>
                    <div ref="operTypeRef" class="chart chart-lg" />
                </el-card>
            </el-col>
        </el-row>

        <!-- 活跃用户 -->
        <el-row :gutter="16" class="chart-row">
            <el-col :span="24">
                <el-card shadow="never">
                    <template #header>
                        <el-icon class="card-icon"><Histogram /></el-icon>
                        <span>活跃用户 TOP5（近7日操作次数）</span>
                    </template>
                    <div ref="activeUserRef" class="chart chart-bar" />
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts" name="Index">
import * as echarts from 'echarts'
import { list as listLogininfor } from '@/api/monitor/logininfor'
import type { SysLogininfor } from '@/api/monitor/logininfor'
import { list as listOperlog } from '@/api/monitor/operlog'
import type { SysOperLog } from '@/api/monitor/operlog'
import { list as listOnline } from '@/api/monitor/online'

/**
 * 首页统计仪表盘：数据来自现有审计/监控接口（登录日志 /audit/logininfor、
 * 操作日志 /audit/oper-log、在线用户 /monitor/online），前端聚合出图。
 * 注意：趋势/分布按单页 500 条明细聚合（脚手架演示粒度，超出部分不计入）；
 * 账号缺审计查询权限时对应图表为空，不阻塞首页。
 */
const loading = ref(false)

const onlineCount = ref(0)
const todayLoginCount = ref(0)
const todayOperCount = ref(0)
const todayLoginFailCount = ref(0)

const loginTrendRef = ref<HTMLDivElement>()
const operTypeRef = ref<HTMLDivElement>()
const activeUserRef = ref<HTMLDivElement>()

const charts: echarts.ECharts[] = []

/** yyyy-MM-dd（本地时区） */
function fmtDate(date: Date): string {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
}

/** 近 n 天日期标签（含今天，升序） */
function lastNDays(n: number): string[] {
    const days: string[] = []
    for (let i = n - 1; i >= 0; i--) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        days.push(fmtDate(d))
    }
    return days
}

const OPER_TYPE_LABELS: Record<number, string> = {
    0: '其它',
    1: '新增',
    2: '修改',
    3: '删除',
    4: '授权',
    5: '导出',
    6: '导入',
    7: '强退',
    8: '生成代码',
    9: '清空数据'
}

function totalOf(result: PromiseSettledResult<{ total?: number } | null>): number {
    return result.status === 'fulfilled' ? (result.value?.total ?? 0) : 0
}

async function loadStatCards() {
    const today = fmtDate(new Date())
    const results = await Promise.allSettled([
        listOnline({ pageNum: 1, pageSize: 1 }),
        listLogininfor({ pageNum: 1, pageSize: 1, beginTime: today, endTime: today }),
        listOperlog({ pageNum: 1, pageSize: 1, beginTime: today, endTime: today }),
        listLogininfor({ pageNum: 1, pageSize: 1, status: '1', beginTime: today, endTime: today })
    ])
    onlineCount.value = totalOf(results[0])
    todayLoginCount.value = totalOf(results[1])
    todayOperCount.value = totalOf(results[2])
    todayLoginFailCount.value = totalOf(results[3])
}

async function loadLoginTrendChart() {
    const days = lastNDays(7)
    const result = await listLogininfor({
        pageNum: 1,
        pageSize: 500,
        beginTime: days[0],
        endTime: days[days.length - 1]
    }).catch(() => null)
    const rows: SysLogininfor[] = result?.rows ?? []

    const okByDay = new Map<string, number>(days.map((d) => [d, 0]))
    const failByDay = new Map<string, number>(days.map((d) => [d, 0]))
    for (const row of rows) {
        const day = String(row.accessTime ?? '').substring(0, 10)
        if (!okByDay.has(day)) continue
        if (row.status === '1') {
            failByDay.set(day, (failByDay.get(day) ?? 0) + 1)
        } else {
            okByDay.set(day, (okByDay.get(day) ?? 0) + 1)
        }
    }

    const chart = echarts.init(loginTrendRef.value!)
    charts.push(chart)
    chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['成功', '失败'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: days.map((d) => d.substring(5)) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
            {
                name: '成功',
                type: 'bar',
                stack: 'login',
                itemStyle: { color: '#67c23a' },
                data: days.map((d) => okByDay.get(d) ?? 0)
            },
            {
                name: '失败',
                type: 'bar',
                stack: 'login',
                itemStyle: { color: '#f56c6c' },
                data: days.map((d) => failByDay.get(d) ?? 0)
            }
        ]
    })
}

async function loadOperTypeChart() {
    const days = lastNDays(7)
    const result = await listOperlog({
        pageNum: 1,
        pageSize: 500,
        beginTime: days[0],
        endTime: days[days.length - 1]
    }).catch(() => null)
    const rows: SysOperLog[] = result?.rows ?? []

    const countByType = new Map<number, number>()
    for (const row of rows) {
        const type = Number(row.businessType ?? 0)
        countByType.set(type, (countByType.get(type) ?? 0) + 1)
    }
    const data = [...countByType.entries()].map(([type, value]) => ({
        name: OPER_TYPE_LABELS[type] ?? `类型${type}`,
        value
    }))

    const chart = echarts.init(operTypeRef.value!)
    charts.push(chart)
    chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}：{c} 次（{d}%）' },
        legend: { bottom: 0 },
        series: [
            {
                name: '操作类型',
                type: 'pie',
                radius: ['35%', '65%'],
                center: ['50%', '45%'],
                avoidLabelOverlap: true,
                itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
                label: { formatter: '{b} {d}%' },
                data: data.length > 0 ? data : [{ name: '暂无数据', value: 0 }]
            }
        ]
    })
}

async function loadActiveUserChart() {
    const days = lastNDays(7)
    const result = await listOperlog({
        pageNum: 1,
        pageSize: 500,
        beginTime: days[0],
        endTime: days[days.length - 1]
    }).catch(() => null)
    const rows: SysOperLog[] = result?.rows ?? []

    const countByUser = new Map<string, number>()
    for (const row of rows) {
        const name = String(row.operName ?? '').trim()
        if (!name) continue
        countByUser.set(name, (countByUser.get(name) ?? 0) + 1)
    }
    const top5 = [...countByUser.entries()]
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5)
        .reverse()

    const chart = echarts.init(activeUserRef.value!)
    charts.push(chart)
    chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '6%', bottom: '3%', containLabel: true },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { type: 'category', data: top5.map(([name]) => name) },
        series: [
            {
                name: '操作次数',
                type: 'bar',
                barMaxWidth: 26,
                itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] },
                label: { show: true, position: 'right' },
                data: top5.map(([, count]) => count)
            }
        ]
    })
}

function resizeCharts() {
    charts.forEach((chart) => chart.resize())
}

onMounted(async () => {
    loading.value = true
    try {
        // 各数据块独立兜底：任一接口失败/无权限不影响其余区块
        await Promise.allSettled([
            loadStatCards(),
            loadLoginTrendChart(),
            loadOperTypeChart(),
            loadActiveUserChart()
        ])
    } finally {
        loading.value = false
    }
    window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
    window.removeEventListener('resize', resizeCharts)
    charts.forEach((chart) => chart.dispose())
    charts.length = 0
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

    .chart {
        width: 100%;

        &.chart-lg {
            height: 340px;
        }

        &.chart-bar {
            height: 260px;
        }
    }
}
</style>
