<script setup lang="ts">
import { ref, computed } from 'vue'
import { Bell, X } from 'lucide-vue-next'
import { getCurrentTheme } from '@/utils/theme'

interface Notification {
  id: string
  title: string
  content: string
  time: string
  isRead: boolean
}

const props = defineProps<{
  notifications: Notification[]
}>()

const emit = defineEmits<{
  read: [id: string]
  showDetail: [notification: Notification]
}>()

const showDropdown = ref(false)
const showModal = ref(false)
const selectedNotification = ref<Notification | null>(null)

const currentTheme = computed(() => getCurrentTheme())

function handleMouseEnter() {
  showDropdown.value = true
}

function handleMouseLeave() {
  showDropdown.value = false
}

function handleNotificationClick(notification: Notification) {
  if (!notification.isRead) {
    emit('read', notification.id)
  }
  selectedNotification.value = notification
  showModal.value = true
  showDropdown.value = false
}

function closeModal() {
  showModal.value = false
  selectedNotification.value = null
}

const unreadCount = computed(() => {
  return props.notifications.filter(n => !n.isRead).length
})
</script>

<template>
  <div class="relative" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
    <button
      class="relative flex items-center gap-2 px-3 py-2 rounded-full transition-colors"
      :class="currentTheme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-gray-100'"
    >
      <div class="relative">
        <Bell class="w-5 h-5" style="color: var(--theme-text);" />
        <span 
          v-if="unreadCount > 0"
          class="absolute -top-1 -right-1 w-4 h-4 bg-red-500 text-white text-xs rounded-full flex items-center justify-center"
        >
          {{ unreadCount > 99 ? '99+' : unreadCount }}
        </span>
        <span 
          v-if="unreadCount > 0"
          class="absolute -top-2 -right-2 w-3 h-3 bg-red-500 rounded-full animate-ping"
        ></span>
      </div>
    </button>

    <div
      v-if="showDropdown"
      class="absolute right-0 top-full mt-2 w-80 sm:w-96 bg-white rounded-lg shadow-xl border z-50"
    >
      <div class="p-4 border-b">
        <h3 class="font-semibold text-base" style="color: #1f2937;">消息通知</h3>
      </div>
      <div class="max-h-80 overflow-y-auto">
        <div 
          v-for="notification in notifications"
          :key="notification.id"
          @click="handleNotificationClick(notification)"
          class="p-4 cursor-pointer border-b last:border-b-0 transition-colors"
          :class="currentTheme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-gray-50'"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <span 
                  v-if="!notification.isRead"
                  class="w-2 h-2 bg-red-500 rounded-full flex-shrink-0"
                ></span>
                <h4 class="font-semibold text-sm" style="color: #111827;">{{ notification.title }}</h4>
              </div>
              <p class="text-xs mt-1" style="color: #6b7280;">{{ notification.time }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/50" @click="closeModal"></div>
    <div class="relative bg-white rounded-lg shadow-xl w-full max-w-lg sm:max-w-2xl max-h-[85vh] overflow-y-auto">
      <div class="sticky top-0 flex items-center justify-between p-4 sm:p-6 border-b bg-white">
        <h3 class="font-bold text-lg sm:text-xl" style="color: #111827;">{{ selectedNotification?.title }}</h3>
        <button @click="closeModal" class="p-2 rounded-full transition-colors" :class="currentTheme === 'dark' ? 'hover:bg-gray-700' : 'hover:bg-gray-100'">
          <X class="w-5 h-5" style="color: #374151;" />
        </button>
      </div>
      <div class="p-4 sm:p-6">
        <p class="text-sm sm:text-base leading-relaxed" style="color: #374151;">{{ selectedNotification?.content }}</p>
        <p class="text-xs sm:text-sm mt-6" style="color: #9ca3af;">{{ selectedNotification?.time }}</p>
      </div>
    </div>
  </div>
</template>
