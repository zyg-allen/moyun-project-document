import type { MessageVO } from '@/types/api'

/**
 * 私信 WebSocket 工具
 *
 * 后端契约：端点 /ws-message?token=xxx（STOMP 协议），订阅 /user/queue/message，
 * 收到消息格式为 MessageVO。
 *
 * 说明：当前项目未引入 @stomp/stompjs，这里采用「原生 WebSocket + STOMP 握手」尝试连接，
 * 若连接失败（浏览器不支持、跨域、握手失败等），自动降级为轮询（每 5 秒拉取一次新消息），
 * 以保证私信实时接收功能可用。
 *
 * 安全提示：token 通过 URL query 传递属于 STOMP 浏览器端的常见限制（无法在握手阶段
 * 自定义 HTTP Header）。生产环境建议：
 *   1. 后端为 ws 握手单独签发短时效的「一次性 token」，握手后立即作废；
 *   2. 或在 Nginx/网关层基于 Cookie 转发鉴权，避免 token 出现在 URL 中。
 * 当前实现仅作最小可用方案，待引入 stompjs 后可改用 Authorization Header。
 *
 * 待引入 stompjs 后，可将 connect 内部替换为 Stomp.client()，轮询可保留为兜底。
 */

const STOMP_SUBSCRIPTION = '/user/queue/message'

export interface MessageWebSocketOptions {
    /** 轮询间隔（毫秒），默认 5000 */
    pollInterval?: number
    /** 自定义 WebSocket 基地址，默认根据当前页面协议推导 */
    wsBaseUrl?: string
}

type MessageHandler = (msg: MessageVO) => void

export class MessageWebSocket {
    private ws: WebSocket | null = null
    private pollingTimer: ReturnType<typeof setInterval> | null = null
    private isConnected = false
    private token = ''
    private onMessage: MessageHandler | null = null
    private pollFn: (() => Promise<void>) | null = null
    private readonly pollInterval: number
    private readonly wsBaseUrl: string
    private subscriptionId = `sub-${Date.now()}`

    constructor(options: MessageWebSocketOptions = {}) {
        this.pollInterval = options.pollInterval ?? 5000
        this.wsBaseUrl = options.wsBaseUrl ?? MessageWebSocket.getDefaultWsBaseUrl()
    }

    private static getDefaultWsBaseUrl(): string {
        if (typeof window === 'undefined') return ''
        const { protocol, host } = window.location
        const wsProtocol = protocol === 'https:' ? 'wss:' : 'ws:'
        return `${wsProtocol}//${host}`
    }

    /** 是否已建立实时连接 */
    get connected(): boolean {
        return this.isConnected
    }

    /**
     * 建立连接
     * @param token 登录 token
     * @param onMessage 收到消息回调
     * @param pollFn 降级轮询时调用的拉取函数（由调用方提供，负责拉取该会话的新消息）
     */
    connect(token: string, onMessage: MessageHandler, pollFn?: () => Promise<void>): void {
        this.token = token
        this.onMessage = onMessage
        this.pollFn = pollFn ?? null

        // 先启动轮询兜底，再尝试 WebSocket（避免 WS 握手期间消息丢失）
        this.startPolling()

        // WebSocket 待引入 stompjs 后启用：这里尝试原生 WS + STOMP 握手
        this.tryConnectWebSocket()
    }

    /** 断开连接，清理所有定时器与连接 */
    disconnect(): void {
        this.stopPolling()
        if (this.ws) {
            try {
                // 发送 STOMP DISCONNECT 帧
                this.sendStompFrame('DISCONNECT')
                this.ws.close()
            } catch {
                /* ignore */
            }
            this.ws = null
        }
        this.isConnected = false
        this.onMessage = null
        this.pollFn = null
    }

    private tryConnectWebSocket(): void {
        if (typeof WebSocket === 'undefined' || !this.token || !this.wsBaseUrl) {
            return
        }

        let ws: WebSocket
        try {
            const url = `${this.wsBaseUrl}/ws-message?token=${encodeURIComponent(this.token)}`
            ws = new WebSocket(url)
        } catch {
            return
        }

        ws.onopen = () => {
            this.ws = ws
            // 发送 STOMP CONNECT 帧
            // heart-beat: 10000,10000 表示「每 10s 发送一次心跳，期望对方每 10s 发送一次」，
            // STOMP 心跳由协议层负责，无需额外应用层心跳帧
            this.sendStompFrame('CONNECT', {
                'accept-version': '1.2',
                'host': window.location.host,
                'heart-beat': '10000,10000',
            })
        }

        ws.onmessage = (event: MessageEvent) => {
            this.handleStompFrame(event.data)
        }

        ws.onerror = () => {
            // WebSocket 异常，保持轮询降级
            this.isConnected = false
        }

        ws.onclose = () => {
            this.isConnected = false
            // 保持轮询兜底，不主动重连（避免无限重连消耗资源）
        }
    }

    private sendStompFrame(command: string, headers: Record<string, string> = {}, body: string = ''): void {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return
        let frame = command.toUpperCase() + '\n'
        Object.entries(headers).forEach(([k, v]) => {
            frame += `${k}:${v}\n`
        })
        frame += '\n'
        if (body) {
            frame += body
        }
        frame += '\x00'
        this.ws.send(frame)
    }

    private handleStompFrame(raw: string): void {
        if (typeof raw !== 'string' || !raw) return
        // STOMP 帧以命令行开头，解析简单判断
        const lines = raw.split('\n')
        const command = lines[0]?.trim().toUpperCase()

        if (command === 'CONNECTED') {
            this.isConnected = true
            // 订阅消息队列
            this.sendStompFrame('SUBSCRIBE', {
                id: this.subscriptionId,
                destination: STOMP_SUBSCRIPTION,
            })
            // WS 连接成功后停止轮询，由 WS 实时推送
            this.stopPolling()
            return
        }

        if (command === 'MESSAGE') {
            // STOMP MESSAGE 帧格式：MESSAGE\nheaders...\n\nbody\x00
            const bodyStart = raw.indexOf('\n\n')
            if (bodyStart === -1) return
            const body = raw.substring(bodyStart + 2).replace(/\x00$/, '').trim()
            if (!body) return
            try {
                const msg = JSON.parse(body) as MessageVO
                this.onMessage?.(msg)
            } catch {
                // 非 JSON 体，忽略
            }
            return
        }
    }

    private startPolling(): void {
        this.stopPolling()
        if (!this.pollFn) return
        this.pollingTimer = setInterval(() => {
            this.pollFn?.().catch(() => {
                /* 轮询单次失败忽略，下次继续 */
            })
        }, this.pollInterval)
    }

    private stopPolling(): void {
        if (this.pollingTimer) {
            clearInterval(this.pollingTimer)
            this.pollingTimer = null
        }
    }
}
