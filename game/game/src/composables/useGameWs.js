import { ref } from 'vue'

const ws = ref(null)
const handlers = new Set()
let reconnectTimer = null
let lastParams = null        // 存上次 connect 的参数
let reconnectAttempts = 0
const MAX_RECONNECT = 10

export function useGameWs() {
  function connect(roomId, userId, side, username, assistantId = 30086009, phase = 'game') {
    // 存参数，重连时用
    lastParams = { roomId, userId, side, username, assistantId, phase }

    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ type: 'JOIN_ROOM', ...lastParams }))
      return
    }

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const isDev = location.hostname === 'localhost'
    const port = isDev ? ':8080' : ''
    ws.value = new WebSocket(`${protocol}//${location.hostname}${port}/ws/game`)

    ws.value.onopen = () => {
      reconnectAttempts = 0                      // 连上了，重置计数
      clearInterval(reconnectTimer)
      reconnectTimer = null
      ws.value.send(JSON.stringify({ type: 'JOIN_ROOM', ...lastParams }))
    }

    ws.value.onmessage = (e) => {
      const msg = JSON.parse(e.data)
      handlers.forEach(h => h(msg))
    }

    ws.value.onclose = () => {
      ws.value = null
      // 如果还有参数（没主动断连），启动重连
      if (lastParams && reconnectAttempts < MAX_RECONNECT) {
        startReconnect()
      }
    }
  }

  function startReconnect() {
    if (reconnectTimer) return
    reconnectTimer = setInterval(() => {
      reconnectAttempts++
      console.log(`重连中... 第 ${reconnectAttempts} 次`)
      if (!lastParams) {
        clearInterval(reconnectTimer)
        reconnectTimer = null
        return
      }
      const { roomId, userId, side, username, assistantId, phase } = lastParams
      connect(roomId, userId, side, username, assistantId, phase)
    }, 3000)  // 每3秒重试一次
  }

  function send(data) {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(data))
    }
  }

  function disconnect() {
    lastParams = null                                 // 主动断连，不重连
    reconnectAttempts = MAX_RECONNECT                 // 阻止重连
    clearInterval(reconnectTimer)
    reconnectTimer = null
    if (ws.value) {
      ws.value.close()
      ws.value = null
    }
  }

  function onMessage(handler) {
    handlers.add(handler)
    return () => handlers.delete(handler)
  }

  return { ws, connect, send, disconnect, onMessage }
}