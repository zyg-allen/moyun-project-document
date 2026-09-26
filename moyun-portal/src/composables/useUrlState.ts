import { watch, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export interface UrlStateItem {
  /** URL query 参数名 */
  key: string
  /** 绑定的响应式状态 */
  state: Ref<string | number | null | undefined>
  /** 值等于这些时从 URL 省略该参数（如页码为 1 时不下发 page） */
  omitValues?: Array<string | number | null | undefined>
  /** 回填时按整数解析 */
  number?: boolean
}

/**
 * 列表页 URL 状态双向绑定（企业级列表页标配）：
 * - 初始化：从当前 route.query 回填状态（在 watch 注册前执行，赋值不触发同步写入）
 * - 同步：状态变化时通过 router.replace 写回 query，自动保留未托管的参数
 *
 * 收益：刷新 / 分享链接 / 浏览器回退时，筛选与页码状态不丢失。
 */
export function useUrlState(items: UrlStateItem[]) {
  const route = useRoute()
  const router = useRouter()

  const isOmitted = (item: UrlStateItem, v: unknown): boolean =>
    v === '' || v === null || v === undefined || (item.omitValues || []).includes(v as any)

  /** 从 route.query 回填到状态（仅覆盖 URL 中显式提供的参数） */
  const restore = () => {
    for (const item of items) {
      const raw = route.query[item.key]
      if (raw === undefined || raw === null || raw === '') continue
      const str = Array.isArray(raw) ? String(raw[0]) : String(raw)
      if (item.number) {
        const n = parseInt(str, 10)
        if (!Number.isNaN(n)) item.state.value = n as never
      } else {
        item.state.value = str as never
      }
    }
  }

  // 初始化先于 watch 注册执行：回填赋值不会触发同步
  restore()

  // 状态变化 → 同步到 URL（保留未托管的 query 参数，如 categoryRecommended / redirect 等）
  watch(
    () => items.map(item => item.state.value),
    () => {
      const query: Record<string, any> = {}
      for (const key of Object.keys(route.query)) {
        if (!items.some(item => item.key === key)) {
          query[key] = route.query[key]
        }
      }
      for (const item of items) {
        const v = item.state.value
        if (!isOmitted(item, v)) {
          query[item.key] = String(v)
        } else {
          delete query[item.key]
        }
      }
      router.replace({ query })
    }
  )

  return { restore }
}
