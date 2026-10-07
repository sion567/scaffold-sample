<template>
  <div class="map-picker">
    <div class="map-picker__body">
      <div class="map-picker__toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索地名或坐标 (lng,lat)"
          size="small"
          clearable
          style="width: 220px"
          @keyup.enter="searchLocation"
        >
          <template #append>
            <el-button :icon="Search" @click="searchLocation" />
          </template>
        </el-input>
        <el-select v-model="mapType" size="small" style="width: 90px" title="切换底图">
          <el-option value="vec" label="矢量" />
          <el-option value="img" label="影像" />
          <el-option value="ter" label="地形" />
        </el-select>
        <el-tag v-if="markerAddress" type="info" size="small" class="map-picker__addr-tag" :title="markerAddress">
          {{ markerAddress }}
        </el-tag>
        <el-button
          v-if="curLng && curLat"
          text
          type="danger"
          size="small"
          @click="clearMarker"
        >清除</el-button>
      </div>
      <div ref="mapContainer" class="map-picker__map" />
      <div class="map-picker__coords">
        <template v-if="curLng && curLat">
          <el-tag type="success" effect="plain" size="small">
            <el-icon><LocationInformation /></el-icon>
            {{ curLng.toFixed(6) }}, {{ curLat.toFixed(6) }}
          </el-tag>
        </template>
        <span v-else class="map-picker__hint">点击地图选择位置，拖动标记精确调整</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, LocationInformation } from '@element-plus/icons-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// 修复 Leaflet 默认图标在 Vue/Vite 项目中的路径问题
// leaflet 未在类型中暴露 _getIconUrl（探测默认图标路径用），此处为官方推荐的构建期处理
  delete (L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown })._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
  iconUrl: new URL('leaflet/dist/images/marker-icon.png', import.meta.url).href,
  shadowUrl: new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).href,
})

// 天地图 Key（去 https://console.tianditu.gov.cn/ 申请，免费）
const TIANDITU_KEY = import.meta.env.VITE_TIANDITU_KEY || ''

// 天地图 WMTS 图层 URL 模板
const TIAN_URL = {
  vec: 'https://t0.tianditu.gov.cn/DataServer?T=vec_c&X={x}&Y={y}&L={z}&tk=',
  img: 'https://t0.tianditu.gov.cn/DataServer?T=img_c&X={x}&Y={y}&L={z}&tk=',
  ter: 'https://t0.tianditu.gov.cn/DataServer?T=ter_c&X={x}&Y={y}&L={z}&tk=',
}

/** 经纬度值:数字、数字字符串或未选择时的 null */
type CoordinateValue = number | string | null

/** change 事件负载 */
interface LocationPayload {
  lng: CoordinateValue
  lat: CoordinateValue
  address: string
}

interface Props {
  lng?: CoordinateValue
  lat?: CoordinateValue
  address?: string
}

const props = withDefaults(defineProps<Props>(), {
  lng: null,
  lat: null,
  address: ''
})

const emit = defineEmits<{
  (e: 'update:lng', value: CoordinateValue): void
  (e: 'update:lat', value: CoordinateValue): void
  (e: 'update:address', value: string): void
  (e: 'change', value: LocationPayload): void
}>()

const keyword = ref('')
const mapType = ref('vec')
const markerAddress = ref(props.address || '')
const curLng = ref<number | null>(props.lng ? Number(props.lng) : null)
const curLat = ref<number | null>(props.lat ? Number(props.lat) : null)
const mapContainer = ref<HTMLElement | null>(null)

let map: L.Map | null = null
let marker: L.Marker | null = null
let tileLayer: L.TileLayer | null = null

// 初始化地图
function initMap() {
  if (map || !mapContainer.value) return

  map = L.map(mapContainer.value, {
    center: [curLng.value && curLat.value ? curLat.value : 29.5637,
              curLng.value && curLat.value ? curLng.value : 106.5507],
    zoom: curLng.value && curLat.value ? 14 : 10,
    zoomControl: true,
    attributionControl: false,
  })

  loadTileLayer()

  // 点击选点
  map.on('click', e => {
    setMarker(e.latlng.lng, e.latlng.lat)
    reverseGeocode(e.latlng.lng, e.latlng.lat)
  })
}

function loadTileLayer() {
  if (tileLayer && map) map.removeLayer(tileLayer)
  const key = TIANDITU_KEY || 'placeholder'  // 无Key时用占位符，下方提示申请
  tileLayer = L.tileLayer(TIAN_URL[mapType.value as keyof typeof TIAN_URL] + key, {
    maxZoom: 18,
    minZoom: 3,
  })
  if (tileLayer && map) tileLayer.addTo(map)
}

// 已有坐标时显示标记
function placeInitialMarker() {
  if (curLng.value && curLat.value) {
    setMarker(curLng.value, curLat.value, false)
  }
}

// 设置或移动标记
function setMarker(lng: number, lat: number, emitChange = true) {
  curLng.value = lng
  curLat.value = lat
  if (emitChange) {
    emit('update:lng', lng)
    emit('update:lat', lat)
  }
  if (map) {
    if (!marker) {
      marker = L.marker([lat, lng], { draggable: true }).addTo(map)
      marker.on('dragend', e => {
        const p = e.target.getLatLng()
        setMarker(p.lng, p.lat, true)
        reverseGeocode(p.lng, p.lat)
      })
    } else {
      marker.setLatLng([lat, lng])
    }
    map.setView([lat, lng], Math.max(map.getZoom(), 14))
  }
}

// 逆地理编码（天地图）
function reverseGeocode(lng: number, lat: number) {
  if (!TIANDITU_KEY) return
  const url = `https://api.tianditu.gov.cn/geocoder?location=${lng},${lat}&type=geocode&tk=${TIANDITU_KEY}`
  fetch(url)
    .then(r => r.json())
    .then(data => {
      if (data.status === '0' && data.result?.formatted_address) {
        const addr = data.result.formatted_address
        markerAddress.value = addr
        emit('update:address', addr)
        emit('change', { lng, lat, address: addr })
      }
    })
    .catch(() => {})
}

// 搜索（天地图地名搜索）
function searchLocation() {
  if (!keyword.value) return
  const text = keyword.value.trim()

  // 如果是 "lng,lat" 格式，直接跳转
  const coordMatch = text.match(/^(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)$/)
  if (coordMatch) {
    const lng = parseFloat(coordMatch[1])
    const lat = parseFloat(coordMatch[2])
    setMarker(lng, lat)
    reverseGeocode(lng, lat)
    return
  }

  if (!TIANDITU_KEY) {
    // 备选：Nominatim（无需Key，有速率限制）
    const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(text)}&format=json&limit=1`
    fetch(url, { headers: { 'Accept-Language': 'zh-CN' } })
      .then(r => r.json())
      .then(data => {
        if (data.length > 0) {
          const { lon, lat, display_name } = data[0]
          setMarker(parseFloat(lon), parseFloat(lat))
          markerAddress.value = display_name
          emit('update:address', display_name)
          emit('change', { lng: parseFloat(lon), lat: parseFloat(lat), address: display_name })
        }
      })
      .catch(() => {})
    return
  }

  // 天地图搜索
  const url = `https://api.tianditu.gov.cn/v2/search?keyWord=${encodeURIComponent(text)}&type=query&tk=${TIANDITU_KEY}`
  fetch(url)
    .then(r => r.json())
    .then(data => {
      if (data.status === '0' && data.pois?.length > 0) {
        const p = data.pois[0]
        const [lng, lat] = p.lonlat.split(',').map(Number)
        setMarker(lng, lat)
        markerAddress.value = p.name + (p.address || '')
        emit('update:address', p.name + (p.address || ''))
        emit('change', { lng, lat, address: p.name + (p.address || '') })
      }
    })
    .catch(() => {})
}

function clearMarker() {
  if (marker && map) {
    map.removeLayer(marker)
    marker = null
  }
  curLng.value = null
  curLat.value = null
  markerAddress.value = ''
  emit('update:lng', null)
  emit('update:lat', null)
  emit('update:address', '')
  emit('change', { lng: null, lat: null, address: '' })
}

watch(mapType, () => {
  if (map) loadTileLayer()
})

onMounted(() => {
  initMap()
  // 等地图容器渲染后再放标记
  nextTick(() => {
    if (curLng.value && curLat.value) placeInitialMarker()
  })
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})

watch(() => props.lng, v => { curLng.value = v ? Number(v) : null })
watch(() => props.lat, v => { curLat.value = v ? Number(v) : null })
watch(() => props.address, v => { markerAddress.value = v || '' })
</script>

<style scoped>
.map-picker {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.map-picker__body {
  display: flex;
  flex-direction: column;
}

.map-picker__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
  flex-wrap: wrap;
}

.map-picker__addr-tag {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.map-picker__map {
  width: 100%;
  height: 320px;
}

.map-picker__coords {
  padding: 6px 10px;
  background: var(--el-fill-color-light);
  border-top: 1px solid var(--el-border-color);
  min-height: 32px;
  display: flex;
  align-items: center;
}

.map-picker__hint {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
</style>
