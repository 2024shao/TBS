d:\暂时文件\实训\Game\game\game\src\components\PositionAdjust.vue
<template>
  <div class="pos-adjust-container">
    <div class="pos-upper">
      <div class="player-info">
        <img :src="avatarUrl(mySide === 0 ? myAssistantId : opponentAssistantId)" class="player-avatar" />
        <span class="player-name">{{ mySide === 0 ? myUsername : opponentUsername }}</span>
      </div>
      <div class="announce-area">
        <div class="announce-text">{{ announceMsg }}</div>
      </div>
      <div class="player-info player-info-right">
        <span class="player-name">{{ mySide === 1 ? myUsername : opponentUsername }}</span>
        <img :src="avatarUrl(mySide === 1 ? myAssistantId : opponentAssistantId)" class="player-avatar" />
      </div>
    </div>

    <div class="pos-up">
      <div class="pos-left-placeholder">
            <div :class="['pos-left', { 'pos-left-visible': hoverRole || pinnedRole }]">
              <img :src="(pinnedRole || hoverRole) ? roleBgUrl((pinnedRole || hoverRole).portraitId) : ''" class="sidebar-portrait" />
            </div>
      </div>
      <div class="pos-lower">
        <div class="pos-title">调整我方阵容位置（点击两个位置互换）</div>
        <div class="pos-body">
          <div class="pos-content">
            <div class="pos-slots">
              <div
                v-for="(roleId, idx) in myOrder"
                :key="'pos-'+idx"
                :class="['pos-slot', {
                  'pos-slot-selected': selectedSlot === idx,
                  'pos-slot-confirmed': confirmed
                }]"
                @click="clickSlot(idx); pinRole(idx)"
                @mouseenter="selectRole(idx)"
                @mouseleave="clearRole"
              >
                <div class="pos-slot-num">{{ idx + 1 }}号位</div>
                <img :src="roleBgUrl(getRole(roleId)?.portraitId)" class="pos-slot-img" />
                <span class="pos-slot-name">{{ getRole(roleId)?.roleName }}</span>
              </div>
            </div>
            <el-button
              class="pos-confirm-btn"
              type="success"
              :disabled="confirmed"
              @click="confirmPosition"
            >
              {{ confirmed ? '已确认位置' : '确认位置' }}
            </el-button>
          </div>
        </div>
      </div>
      <div class="pos-right-placeholder">
          <div :class="['pos-right', { 'pos-right-visible': hoverRole || pinnedRole }]" @mouseenter="onPanelEnter" @mouseleave="clearRole">
            <el-button class="detail-close" @click="clearPinned" circle>✕</el-button>
          <div v-if="pinnedRole || hoverRole" class="role-info">
            <h3>{{ (pinnedRole || hoverRole).roleName }}</h3>
            <div class="info-stats">
              <p>生命值：{{ (pinnedRole || hoverRole).hp }} / {{ (pinnedRole || hoverRole).hpMax }}</p>
              <p>攻击力：{{ (pinnedRole || hoverRole).attack }}</p>
            </div>
            <div class="info-desc">
              <p><strong>技能：</strong>{{ (pinnedRole || hoverRole).skillDescription || '无' }}</p>
              <p><strong>被动：</strong>{{ (pinnedRole || hoverRole).passiveDescription || '无' }}</p>
            </div>
            <div v-if="(pinnedRole || hoverRole).remark" class="info-remark">
              <p>{{ (pinnedRole || hoverRole).remark }}</p>
            </div>
          </div>
          <div v-else class="info-placeholder">悬停或点击角色查看信息</div>
          </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, toRefs, computed } from 'vue'
import { roleBgUrl, avatarUrl } from '../config/cdn'
import { getAllRoles } from '../api/game'
import { gameState, doPhaseTransition } from '../composables/useGameState'
import { useGameWs } from '../composables/useGameWs'

const { roomId, userId, mySide, myUsername, myAssistantId, opponentUsername, opponentAssistantId } = toRefs(gameState)
const myPicks = computed(() => gameState.myPicks || [])
const myOrder = ref([...myPicks.value])
const selectedSlot = ref(-1)
const confirmed = ref(false)
const announceMsg = ref('请调整我方阵容位置')
const roles = ref([])
const { connect, send, onMessage } = useGameWs()

// 注册消息处理
const unregister = onMessage((msg) => {
  switch (msg.type) {
    case 'POSITION_CONFIRMED':
      if (msg.userId !== userId.value) announceMsg.value = '对方已确认位置'
      break
    case 'POSITION_PHASE_COMPLETE':
      gameState.roleIds = msg.roleIds.join(' ')
      gameState.sessionId = roomId.value + '_' + Date.now()
      doPhaseTransition('game')
      break
    case 'PLAYER_LEFT':
      alert('对方已离开')
      gameState.phase = 'none'
      break
    case 'PLAYER_RECONNECTED':
      if (msg.userId !== userId.value) {
        ElMessage.success('对方已重连')
      }
      break
  }
})

function getRole(roleId) {
  return roles.value.find(r => r.roleId === roleId)
}

function clickSlot(idx) {
  if (confirmed.value) return
  if (selectedSlot.value === -1) {
    selectedSlot.value = idx
  } else if (selectedSlot.value === idx) {
    selectedSlot.value = -1
  } else {
    const arr = [...myOrder.value]
    const temp = arr[selectedSlot.value]
    arr[selectedSlot.value] = arr[idx]
    arr[idx] = temp
    myOrder.value = arr
    selectedSlot.value = -1
  }
}

const hoverRole = ref(null)
const pinnedRole = ref(null)
let leaveTimer = null

function selectRole(idx) {
  const role = getRole(myOrder.value[idx])
  if (!role) return
  if (leaveTimer) clearTimeout(leaveTimer)
  hoverRole.value = role
}

function pinRole(idx) {
  const role = getRole(myOrder.value[idx])
  if (!role) return
  pinnedRole.value = role
  if (leaveTimer) clearTimeout(leaveTimer)
}

function clearRole() {
  if (pinnedRole.value) return
  leaveTimer = setTimeout(() => {
    hoverRole.value = null
  }, 200)
}

function clearPinned() {
  pinnedRole.value = null
  hoverRole.value = null
}

function onPanelEnter() {
  if (leaveTimer) clearTimeout(leaveTimer)
}

function confirmPosition() {
  send({
    type: 'CONFIRM_POSITION',
    roomId: roomId.value,
    userId: userId.value,
    role: mySide.value,
    order: [...myOrder.value]
  })
  confirmed.value = true
  announceMsg.value = '已确认位置，等待对方...'
}

onMounted(async () => {
  const res = await getAllRoles()
  roles.value = Array.isArray(res) ? res : (res.data || [])
  connect(roomId.value, userId.value, mySide.value, myUsername.value)
})

onUnmounted(() => {
  unregister();
  if (gameState.phase === 'none') {
    send({ type: 'LEAVE_ROOM', roomId: roomId.value, userId: userId.value })
  }
})
</script>

<style scoped>
.pos-adjust-container {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, #0a0a2e 0%, #1a1a3e 100%);
  display: flex; flex-direction: column;
  color: #fff;
}

.pos-upper {
  height: 130px;
  display: flex; align-items: center;
  justify-content: space-between;
  padding: 0 38px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.player-info {
  display: flex; align-items: center; gap: 15px;
}
.player-info-right {
  flex-direction: row-reverse;
}
.player-avatar {
  width: 44px; height: 44px;
  border: 2px solid rgba(255,255,255,0.2);
  object-fit: contain;
  background: #fff;
}
.player-name {
  font-size: 19px; color: rgba(255,255,255,0.85);
}

.announce-area {
  flex: 1; text-align: center;
  padding: 0 38px;
  border-left: 2px solid rgba(255,255,255,0.15);
  border-right: 2px solid rgba(255,255,255,0.15);
}
.announce-text {
  font-size: 23px; color: #ffd700;
}

.pos-up {
  display: flex;
  flex: 1;
  min-height: 0;
}

.pos-lower {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 16px;
  position: relative;
  padding: 0 20px;
}

.pos-body {
  display: flex; align-items: center; justify-content: center;
  gap: 0; width: 100%;
}

.pos-content {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 32px;
  flex: 1;
}

.pos-left-placeholder {
  width: 320px;  flex-shrink: 0;
  position: relative;
  border-radius: 8px 0 0 8px; overflow: hidden;
  background: rgba(0,0,0,0.15);
}

.pos-left {
  width: 100%; height: 100%;
  position: absolute; top: 0; left: 0;
  transform: translateX(-100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.pos-left-visible {
  transform: translateX(0);
}

.sidebar-portrait {
  width: 100%; height: 100%; object-fit: cover; object-position: top center;
}

.pos-right-placeholder {
  width: 320px; flex-shrink: 0;
  position: relative;
  border-radius: 0 8px 8px 0; overflow: hidden;
  background: rgba(0,0,0,0.15);
}

.pos-right {
  width: 100%; height: 100%;
  background: rgba(30, 58, 95, 0.95);
  padding: 16px;
  display: flex; flex-direction: column; justify-content: flex-start;
  overflow-y: auto; scrollbar-width: none;
  position: absolute; top: 0; right: 0;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.pos-right-visible {
  transform: translateX(0);
  box-shadow: -4px 0 16px rgba(0,0,0,0.5);
}
.pos-right::-webkit-scrollbar { display: none; }

.detail-close {
  position: absolute; top: 8px; right: 8px; z-index: 1;
}
.role-info h3 {
  font-size: 60px; margin-bottom: 10px; color: #ffd54f;
}
.info-placeholder {
  color: #555; font-size: 36px; text-align: center;
  margin-top: 40px;
}
.info-stats p {
  margin: 4px 0; color: #aaa; font-size: 30px;
}
.info-desc {
  margin-top: 12px;
}
.info-desc p {
  margin: 4px 0; color: #bbb; font-size: 30px;
}
.info-remark {
  margin-top: 12px;
  border-top: 1px solid rgba(255,255,255,0.1);
  padding-top: 8px;
}
.info-remark p {
  color: #888; font-size: 20px; font-style: italic;
}

.pos-title {
  font-size: 25px; color: rgba(255,255,255,0.7);
  margin-bottom: 11px;
}

.pos-slots {
  display: flex; gap: 29px;
}

.pos-slot {
  width: 192px; height: 307px;
  background: rgba(255,255,255,0.05);
  border: 2px solid rgba(255,255,255,0.15);
  border-radius: 10px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.pos-slot:hover {
  border-color: rgba(255,255,255,0.4);
  background: rgba(255,255,255,0.1);
}
.pos-slot-selected {
  border-color: #ffd700;
  box-shadow: 0 0 19px rgba(255,215,0,0.4);
}
.pos-slot-confirmed {
  opacity: 0.6;
  pointer-events: none;
  border-color: #4caf50;
}
.pos-slot-num {
  font-size: 15px; color: rgba(255,255,255,0.5);
}
.pos-slot-img {
  width: 134px; height: 173px;
  object-fit: contain;
  background: #fff;
  border-radius: 6px;
}
.pos-slot-name {
  font-size: 15px; color: rgba(255,255,255,0.9);
}

.pos-confirm-btn {
  width: 230px; height: 58px;
  font-size: 19px;
}
</style>