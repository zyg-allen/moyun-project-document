import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 图片懒加载 hook
 */
export function useLazyLoad() {
  const observer = ref<IntersectionObserver | null>(null)

  onMounted(() => {
    if (typeof window !== 'undefined' && 'IntersectionObserver' in window) {
      observer.value = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              const img = entry.target as HTMLImageElement
              const src = img.dataset.src
              if (src) {
                img.src = src
                img.classList.remove('loading')
                img.classList.add('loaded')
                observer.value?.unobserve(img)
              }
            }
          })
        },
        {
          rootMargin: '50px 0px',
          threshold: 0.01
        }
      )
    }
  })

  onUnmounted(() => {
    observer.value?.disconnect()
  })

  const observe = (el: HTMLElement) => {
    if (observer.value && el) {
      observer.value.observe(el)
    }
  }

  const unobserve = (el: HTMLElement) => {
    if (observer.value && el) {
      observer.value.unobserve(el)
    }
  }

  return { observe, unobserve }
}

/**
 * 防抖函数
 */
export function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null
  return function (this: any, ...args: Parameters<T>) {
    if (timeoutId) clearTimeout(timeoutId)
    timeoutId = setTimeout(() => fn.apply(this, args), delay)
  }
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: any[]) => any>(
  fn: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle = false
  return function (this: any, ...args: Parameters<T>) {
    if (!inThrottle) {
      fn.apply(this, args)
      inThrottle = true
      setTimeout(() => (inThrottle = false), limit)
    }
  }
}

/**
 * 视口尺寸 hook
 */
export function useViewport() {
  const width = ref(typeof window !== 'undefined' ? window.innerWidth : 1920)
  const height = ref(typeof window !== 'undefined' ? window.innerHeight : 1080)

  const updateSize = () => {
    if (typeof window !== 'undefined') {
      width.value = window.innerWidth
      height.value = window.innerHeight
    }
  }

  onMounted(() => {
    window.addEventListener('resize', debounce(updateSize, 200))
  })

  onUnmounted(() => {
    window.removeEventListener('resize', debounce(updateSize, 200))
  })

  return { width, height }
}

/**
 * 文档标题管理
 */
export function useDocumentTitle(defaultTitle: string = '墨韵') {
  const setTitle = (title: string) => {
    if (typeof document !== 'undefined') {
      document.title = title ? `${title} - ${defaultTitle}` : defaultTitle
    }
  }

  return { setTitle }
}

/**
 * 页面可见性 hook
 */
export function usePageVisibility() {
  const isVisible = ref(
    typeof document !== 'undefined' ? !document.hidden : true
  )

  const handleVisibilityChange = () => {
    if (typeof document !== 'undefined') {
      isVisible.value = !document.hidden
    }
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })

  onUnmounted(() => {
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  return { isVisible }
}
