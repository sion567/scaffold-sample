/**
 * 坐标系转换工具
 *
 * 国内常见坐标系：
 * - WGS84    国际标准，GPS设备原始坐标
 * - GCJ-02   国测局坐标（火星坐标），天地图/高德/腾讯/谷歌中国使用
 * - BD-09    百度坐标（百度地图专用）
 *
 * 天地图使用 CGJ-2000，与 GCJ-02 偏差极小（约数厘米），可直接混用
 * 从 BD-09 / GCJ-02 坐标转到天地图（CGJ-2000）用 BD-09→GCJ-02 转换
 */

/** 经纬度坐标点 */
export interface LngLatPoint {
  lng: number
  lat: number
}

const PI = 3.1415926535897932384626
const A = 6378245.0
const EE = 0.00669342162296594723

/**
 * BD-09（百度）→ GCJ-02（天地图/高德）
 * @param {number} bdLng
 * @param {number} bdLat
 * @returns {{ lng: number, lat: number }}
 */
export function bd09ToGcj02(bdLng: number, bdLat: number): LngLatPoint {
  const x = bdLng - 0.0065
  const y = bdLat - 0.006
  const z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * PI * 3000.0 / 180.0)
  const theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * PI * 3000.0 / 180.0)
  return {
    lng: z * Math.cos(theta),
    lat: z * Math.sin(theta),
  }
}

/**
 * GCJ-02（火星坐标）→ BD-09（百度）
 * @param {number} gcjLng
 * @param {number} gcjLat
 * @returns {{ lng: number, lat: number }}
 */
export function gcj02ToBd09(gcjLng: number, gcjLat: number): LngLatPoint {
  const z = Math.sqrt(gcjLng * gcjLng + gcjLat * gcjLat) + 0.00002 * Math.sin(gcjLat * PI * 3000.0 / 180.0)
  const theta = Math.atan2(gcjLat, gcjLng) + 0.000003 * Math.cos(gcjLng * PI * 3000.0 / 180.0)
  return {
    lng: z * Math.cos(theta) + 0.0065,
    lat: z * Math.sin(theta) + 0.006,
  }
}

/**
 * WGS84 → GCJ-02
 * @param {number} wgsLng
 * @param {number} wgsLat
 * @returns {{ lng: number, lat: number }}
 */
export function wgs84ToGcj02(wgsLng: number, wgsLat: number): LngLatPoint {
  return transform(wgsLng, wgsLat)
}

function transform(lng: number, lat: number): LngLatPoint {
  let dlat = transformLat(lng - 105.0, lat - 35.0)
  let dlng = transformLng(lng - 105.0, lat - 35.0)
  const radlat = (lat / 180.0) * PI
  let magic = Math.sin(radlat)
  magic = 1 - EE * magic * magic
  const sqrtmagic = Math.sqrt(magic)
  dlat = (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * PI)
  dlng = (dlng * 180.0) / (A / sqrtmagic * Math.cos(radlat) * PI)
  return { lng: lng + dlng, lat: lat + dlat }
}

function transformLat(x: number, y: number) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(y / 12.0 * PI) + 320.0 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
  return ret
}

function transformLng(x: number, y: number) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
  return ret
}

/**
 * 判断坐标是否在中国境外（在国内才需要转换）
 * @param {number} lng
 * @param {number} lat
 */
function outOfChina(lng: number, lat: number) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}
