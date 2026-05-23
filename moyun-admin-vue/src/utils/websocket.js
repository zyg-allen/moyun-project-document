import useWsdataStore from '@/store/modules/wsdata'
import UserStore from '@/store/modules/user'

async function useWebsocketCto() {
    var websocketCto = {
        wsConn: null,     // 链接的对象
        url: import.meta.env.VITE_APP_BASE_WS + '/websocket/message', // 链接的地址
        syncLock: false,  // 锁
        timeout: 60 * 1000 * 5,
        timeoutObj: null,
        timeoutNum: null,

        // GetWsConn 获得链接对象
        GetWsConn: function () {
            if (websocketCto.wsConn == null) {
                websocketCto.Init()
            }
            return websocketCto.wsConn
        },

        // Init 初始化链接对象
        Init: function () {
            console.log("ws init")
            if ("WebSocket" in window) {
                this.wsConn = new WebSocket(this.url);
            } else if ("MozWebSocket" in window) {
                this.wsConn = new MozWebSocket(url);
            } else {
                console.log("您的浏览器不支持 WebSocket!");
                return;
            }
            this.wsConn.onopen = this.onopen
            this.wsConn.onclose = this.onclose
            this.wsConn.onmessage = this.onmessage
            this.wsConn.onerror = this.onerror
        },

        onmessage: function (e) {
            websocketCto.startHeart()
            if (e.data == 'ok') {//心跳消息不做处理
                return
            }
            // 处理返回的数据
            messageHandle(e.data)
        },

        onclose: function () {
            websocketCto.reconnect();
        },

        onopen: function () {
            websocketCto.startHeart();
        },

        onerror: function (e) {
            websocketCto.reconnect()
        },

        reconnect: function () {
            if (this.syncLock) {
                return;
            }
            this.syncLock = true,
                this.timeoutNum && clearTimeout(this.timeoutNum)
            this.timeoutNum = setTimeout(function () {
                websocketCto.Init();
                this.syncLock = false;
            }, 1000)
        },

        startHeart: function () {
            this.timeoutObj && clearInterval(this.timeoutObj)
            this.timeoutObj = setInterval(function () {
                let beBase = {
                    cmd: 10101,
                    data: ''
                }
                websocketCto.wsConn.send(JSON.stringify(beBase));
                if (websocketCto.wsConn.readyState != 1) {
                    websocketCto.reconnect()
                }
            }, this.timeout)
        },

        resetHeart: function () {
            clearInterval(this.timeoutObj)
            this.startHeart()
        }
    }
    return websocketCto;
}


export default useWebsocketCto;

//根据消息标识做不同的处理
function messageHandle(message) {
    const wsdataStore = useWsdataStore()

    // const newResponseString = message.replace(/\"id\": (\d+)/,'"id":"$1"');
    console.log('ws接到数据', message)
    try {
        const json = JSON.parse(message)
        if (json.cmd) return
        wsdataStore.setWsJson(json)
    } catch (e) {
        wsdataStore.setWsMessage(message)
    }

}
