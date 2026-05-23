import { ref, onMounted, onUnmounted, watch } from 'vue'

export function useLazyImage() {
  const imageRefs = ref<(HTMLImageElement | null)[]>([])
  const observer = ref<IntersectionObserver | null>(null)

  const initObserver = () => {
    if (typeof window === 'undefined') return

    observer.value = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const img = entry.target as HTMLImageElement
            const dataSrc = img.getAttribute('data-src')
            if (dataSrc) {
              img.src = dataSrc
              img.classList.add('loaded')
              observer.value?.unobserve(img)
            }
          }
        })
      },
      {
        rootMargin: '100px 0px',
        threshold: 0.01
      }
    )

    imageRefs.value.forEach((img) => {
      if (img && observer.value) {
        observer.value.observe(img)
      }
    })
  }

  const addImageRef = (el: HTMLImageElement | null) => {
    if (el && !imageRefs.value.includes(el)) {
      imageRefs.value.push(el)
      if (observer.value) {
        observer.value.observe(el)
      }
    }
  }

  onMounted(() => {
    initObserver()
  })

  onUnmounted(() => {
    if (observer.value) {
      observer.value.disconnect()
    }
  })

  return {
    addImageRef
  }
}
