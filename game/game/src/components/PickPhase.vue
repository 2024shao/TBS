<template>
  <div class="pick-container">
    <!-- 上区域：双方阵容 + 禁用 -->
    <div class="pick-upper">
      <div class="team-panel team-0">
        <div class="player-info">
          <img :src="avatarUrl(mySide === 0 ? myAssistantId : opponentAssistantId)" class="player-avatar" />
          <span class="player-name">{{ mySide === 0 ? myUsername : opponentUsername }}</span>
        </div>
        <div class="team-slots">
          <div v-for="i in 5" :key="'t0-'+i" class="team-slot">
            <img v-if="team0Picks[i-1]" :src="avatarUrl(getPortraitId(team0Picks[i-1]))" class="team-slot-img" />
            <div v-else class="team-slot-empty">{{ i }}</div>
          </div>
        </div>
        <div class="ban-row">
          <span class="ban-label">禁用</span>
          <div v-for="i in 3" :key="'b0-'+i" class="ban-slot">
            <img v-if="team0Bans[i-1]" :src="avatarUrl(getPortraitId(team0Bans[i-1]))" class="ban-slot-img ban-img" />
            <div v-else class="ban-slot-empty">×</div>
          </div>
        </div>
      </div>
      <div class="announce-area">
        <div class="announce-text">{{ announceMsg }}</div>
      </div>
      <div class="team-panel team-1">
        <div class="player-info player-info-right">
          <span class="player-name">{{ mySide === 1 ? myUsername : opponentUsername }}</span>
          <img :src="avatarUrl(mySide === 1 ? myAssistantId : opponentAssistantId)" class="player-avatar" />
        </div>
        <div class="team-slots">
          <div v-for="i in 5" :key="'t1-'+i" class="team-slot">
            <img v-if="team1Picks[i-1]" :src="avatarUrl(getPortraitId(team1Picks[i-1]))" class="team-slot-img" />
            <div v-else class="team-slot-empty">{{ i }}</div>
          </div>
        </div>
        <div class="ban-row">
          <span class="ban-label">禁用</span>
          <div v-for="i in 3" :key="'b1-'+i" class="ban-slot">
            <img v-if="team1Bans[i-1]" :src="avatarUrl(getPortraitId(team1Bans[i-1]))" class="ban-slot-img ban-img" />
            <div v-else class="ban-slot-empty">×</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下区域：三栏 -->
    <div class="pick-lower">
      <div class="lower-left">
        <div v-for="r in roles" :key="'portrait-'+r.roleId"
          :class="['portrait-window', { 'portrait-active': selectedRoleId === r.roleId }]">
          <img :src="roleBgUrl(r.portraitId)" class="portrait-img" />
        </div>
      </div>

      <div class="lower-center">
        <div class="role-grid">
          <div
            v-for="role in roles"
            :key="role.roleId"
            :class="['role-card', {
              'role-card-picked': isPicked(role.roleId),
              'role-card-banned': isBanned(role.roleId),
              'role-card-disabled': !isMyTurn,
              'role-card-hover': selectedRoleId === role.roleId,
              'role-card-selected': tempSelection.includes(role.roleId)
            }]"
            @click="selectRole(role)"
          >
            <img :src="avatarUrl(role.portraitId)" class="role-card-img" />
            <span class="role-card-name">{{ role.roleName }}</span>
          </div>
        </div>
      </div>

      <div class="lower-right">
        <div v-if="selectedRole" class="role-info">
          <h3>{{ selectedRole.roleName }}</h3>
          <div class="info-stats">
            <p>生命值：{{ selectedRole.hp }} / {{ selectedRole.hpMax }}</p>
            <p>攻击力：{{ selectedRole.attack }}</p>
          </div>
          <div class="info-desc">
            <p><strong>技能：</strong>{{ selectedRole.skillDescription || '无' }}</p>
            <p><strong>被动：</strong>{{ selectedRole.passiveDescription || '无' }}</p>
          </div>
          <div v-if="selectedRole.remark" class="info-remark">
            <p>{{ selectedRole.remark }}</p>
          </div>
        </div>
        <div v-else class="info-placeholder">点击角色查看信息</div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="pick-footer">
      <el-button :disabled="!isMyTurn || currentAction !== 0 || tempSelection.length !== currentCount" type="primary" @click="confirmPick">
        确认选择
      </el-button>
      <el-button :disabled="!isMyTurn || currentAction !== 1 || tempSelection.length !== currentCount" type="warning" @click="confirmBan">
        确认禁用
      </el-button>
      <el-button :disabled="!allTurnsDone || myRosterConfirmed" type="success" @click="confirmRoster">
        {{ myRosterConfirmed ? '已确认阵容' : '确认阵容' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { roleBgUrl, avatarUrl } from '../config/cdn'
import { getAllRoles } from '../api/game'
import { gameState, doPhaseTransition } from '../composables/useGameState'
import { useGameWs } from '../composables/useGameWs'

const roomId = computed(() => gameState.roomId)
const userId = computed(() => gameState.userId)
const mySide = computed(() => gameState.mySide)
const myUsername = computed(() => gameState.myUsername)
const myAssistantId = computed(() => gameState.myAssistantId)
const opponentUsername = computed(() => gameState.opponentUsername)
const opponentAssistantId = computed(() => gameState.opponentAssistantId)
const { connect, send, onMessage } = useGameWs()

const roles = ref([])
const team0Picks = ref([])
const team1Picks = ref([])
const team0Bans = ref([])
const team1Bans = ref([])
const tempSelection = ref([])
const selectedRoleId = ref(null)
const currentTurn = ref(1)
const currentAction = ref(0)
const currentCount = ref(1)
const currentSide = ref(-1)
const announceMsg = ref('等待开始...')
const allTurnsDone = ref(false)
const myRosterConfirmed = ref(false)

const isMyTurn = computed(() => {
  if (allTurnsDone.value) return false
  return currentSide.value === mySide.value
})
const selectedRole = computed(() => {
  return roles.value.find(r => r.roleId === selectedRoleId.value) || null
})

// 注册消息处理
const unregister = onMessage((msg) => {
  switch (msg.type) {
    case 'TURN_CHANGE':
      currentTurn.value = msg.turn
      currentAction.value = msg.action
      currentCount.value = msg.count
      currentSide.value = msg.side
      announceMsg.value = msg.message
      break
    case 'TURN_COMPLETE':
      if (msg.action === 'pick') {
        if (msg.side === 0) team0Picks.value = [...team0Picks.value, ...msg.roleIds]
        else team1Picks.value = [...team1Picks.value, ...msg.roleIds]
      } else {
        if (msg.side === 0) team0Bans.value = [...team0Bans.value, ...msg.roleIds]
        else team1Bans.value = [...team1Bans.value, ...msg.roleIds]
      }
      break
    case 'ALL_TURNS_DONE':
      allTurnsDone.value = true
      announceMsg.value = '请确认阵容'
      break
    case 'ROSTER_CONFIRMED':
      if (msg.userId !== userId.value) announceMsg.value = '对方已确认阵容'
      break
    case 'PICK_PHASE_COMPLETE':
      gameState.myPicks = mySide.value === 0 ? msg.hostPicks : msg.guestPicks
      doPhaseTransition('position')
      break
    case 'PLAYER_LEFT':
      announceMsg.value = '对方已离开'
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

function isPicked(roleId) {
  return team0Picks.value.includes(roleId) || team1Picks.value.includes(roleId)
}
function isBanned(roleId) {
  return team0Bans.value.includes(roleId) || team1Bans.value.includes(roleId)
}
function getPortraitId(roleId) {
  const r = roles.value.find(r => r.roleId === roleId)
  return r ? r.portraitId : null
}

function selectRole(role) {
  selectedRoleId.value = role.roleId
  if (!isMyTurn.value || allTurnsDone.value) return
  if (isPicked(role.roleId) || isBanned(role.roleId)) return
  const idx = tempSelection.value.indexOf(role.roleId)
  if (idx >= 0) {
    tempSelection.value.splice(idx, 1)
  } else if (tempSelection.value.length < currentCount.value) {
    tempSelection.value.push(role.roleId)
  }
}

function confirmPick() {
  send({
    type: 'CONFIRM_PICK', roomId: roomId.value, userId: userId.value, role: mySide.value, picks: [...tempSelection.value]
  })
  tempSelection.value = []
}
function confirmBan() {
  send({
    type: 'CONFIRM_BAN', roomId: roomId.value, userId: userId.value, role: mySide.value, bans: [...tempSelection.value]
  })
  tempSelection.value = []
}
function confirmRoster() {
  send({ type: 'CONFIRM_ROSTER', roomId: roomId.value, userId: userId.value, role: mySide.value })
  myRosterConfirmed.value = true
}

onMounted(async () => {
  const res = await getAllRoles()
  roles.value = Array.isArray(res) ? res : (res.data || [])
  connect(roomId.value, userId.value, mySide.value, myUsername.value)
})

onUnmounted(() => {
  unregister()//取消注册，不关连接
})
</script>

<style scoped>
.pick-container {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, #0a0a1a 0%, #1a1a2e 100%);
  display: flex; flex-direction: column;
  color: #fff;
  overflow: hidden;
}

/* === 上区域：双方阵容 === */
.pick-upper {
  display: flex; gap: 0;
  height: 324px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.team-panel {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 16px 15px;
}
.team-1 { border-left: 1px solid rgba(255,255,255,0.1); }
.announce-area {
  width: 159px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-left: 1px solid rgba(255,255,255,0.15);
  border-right: 1px solid rgba(255,255,255,0.15);
}
.announce-text {
  font-size: 18px; font-weight: bold; color: #ffd54f;
  text-align: center;
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.player-info {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 12px;
}
.player-info-right { justify-content: flex-end; }
.player-avatar {
  width: 44px; height: 44px;
  border: 2px solid rgba(255,255,255,0.2);
  object-fit: contain;
  background: #fff;
}
.player-name {
  font-size: 16px; color: #eee;
}
.team-slots { display: flex; gap: 10px; }
.team-slot {
  width: 72px; height: 72px;
  border: 2px solid rgba(255,255,255,0.15);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.03);
}
.team-slot-img { width: 100%; height: 100%; object-fit: contain; }
.team-slot-empty { color: #444; font-size: 16px; }
.ban-row {
  display: flex; align-items: center; gap: 6px;
  margin-top: 15px;
}
.ban-label { font-size: 12px; color: #e57373; }
.ban-slot {
  width: 44px; height: 44px;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.02);
}
.ban-slot-img { width: 100%; height: 100%; object-fit: contain; }
.ban-img { filter: grayscale(1) opacity(0.5); }
.ban-slot-empty { color: #e57373; font-size: 13px; }

/* === 下区域：三栏 === */
.pick-lower {
  flex: 1; display: flex;
  overflow: hidden;
}

/* 左：立绘窗口 */
.lower-left {
  width: 25%; position: relative; overflow: hidden;
  border-right: 1px solid rgba(255,255,255,0.1);
}
.portrait-window {
  position: absolute; top: 0; left: 0;
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: #fff;
  transform: translateX(-100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.portrait-active {
  transform: translateX(0);
  z-index: 1;
}
.portrait-img { max-width: 100%; max-height: 100%; object-fit: contain; }

/* 中：角色列表 */
.lower-center {
  flex: 1; overflow-y: auto;
  padding: 15px;
}
.role-grid {
  display: flex; flex-wrap: wrap; gap: 10px;
  align-content: flex-start; justify-content: center;
}
.role-card {
  display: flex; flex-direction: column; align-items: center;
  width: 90px; padding: 8px;
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(255,255,255,0.03);
}
.role-card:hover { border-color: rgba(255,255,255,0.3); background: rgba(255,255,255,0.08); }
.role-card-hover { background: rgba(255,255,255,0.06); }
.role-card-selected { border-color: #66bb6a; background: rgba(102,187,106,0.15); }
.role-card-picked { border-color: #ffd54f; background: rgba(255,213,79,0.1); }
.role-card-banned { border-color: #e57373; background: rgba(229,115,115,0.1); opacity: 0.5; pointer-events: none; }
.role-card-disabled { opacity: 0.4; }
.role-card-locked { opacity: 0.4; pointer-events: none; }
.role-card-img { width: 63px; height: 63px; object-fit: contain; }
.role-card-name { font-size: 11px; color: #aaa; margin-top: 4px; text-align: center; }

/* 右：角色信息 */
.lower-right {
  width: 25%; overflow-y: auto;
  border-left: 1px solid rgba(255,255,255,0.1);
  padding: 19px;
}
.role-info h3 { font-size: 20px; margin-bottom: 12px; }
.info-stats p { margin: 4px 0; color: #aaa; font-size: 14px; }
.info-desc { margin-top: 12px; }
.info-desc p { margin: 4px 0; color: #bbb; font-size: 13px; }
.info-remark {
  margin-top: 12px; padding: 8px 12px;
  background: rgba(255,255,255,0.05);
  border-radius: 4px;
}
.info-remark p { color: #888; font-size: 12px; font-style: italic; }
.info-placeholder { color: #555; font-size: 14px; text-align: center; }

/* === 底部 === */
.pick-footer {
  padding: 15px; border-top: 1px solid rgba(255,255,255,0.1);
  text-align: center; display: flex; gap: 10px; justify-content: center;
}
.pick-footer .el-button { height: 44px; font-size: 16px; }
.pick-container *:focus { outline: none; }
.pick-container .el-card:focus-visible { outline: none; box-shadow: none; }
</style>