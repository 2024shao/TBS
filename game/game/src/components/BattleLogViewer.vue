<template>
  <div class="battle-viewer">
    <div class="battle-inner" ref="battleInner" :style="innerStyle">
    <transition name="control-slide">
      <div class="control-panel" v-show="showControl">
        <el-card class="control-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="8">
          <el-input v-model="roleInput" placeholder="输入角色编号，空格分隔，如 1 2 3 4 5 6 7 8 9 10" clearable />
        </el-col>
        <el-col :span="3">
          <el-button type="success" @click="startBattle" :loading="loading">开始战斗</el-button>
        </el-col>
        <el-col :span="5">
          <el-input v-model="sessionId" placeholder="或输入已有 Session ID" clearable />
        </el-col>
        <el-col :span="3">
          <el-button type="primary" @click="loadBattle" :loading="loading">加载战斗</el-button>
        </el-col>
        <el-col :span="8" v-if="steps.length > 0">
          <el-button-group>
            <el-button @click="stepTo(0)" :disabled="currentStep === 0">⏮</el-button>
            <el-button @click="stepBack" :disabled="currentStep === 0">◀</el-button>
            <el-button @click="autoPlay" :type="autoPlaying ? 'warning' : 'success'">
              {{ autoPlaying ? '⏸ 暂停' : '▶ 自动播放' }}
            </el-button>
            <el-button @click="stepForward" :disabled="currentStep >= steps.length - 1">▶</el-button>
            <el-button @click="stepTo(steps.length - 1)" :disabled="currentStep >= steps.length - 1">⏭</el-button>
          </el-button-group>
        </el-col>
        <el-col :span="3">
          <el-button @click="toggleLog" :type="showLog ? 'warning' : 'info'" :disabled="steps.length === 0">
            {{ showLog ? '隐藏日志' : '查看日志' }}
          </el-button>
        </el-col>
        <el-col :span="4" v-if="steps.length > 0">
          <el-slider v-model="currentStep" :min="0" :max="steps.length - 1" :step="1" show-input />
        </el-col>
      </el-row>
        </el-card>
      </div>
    </transition>

    <div class="battle-area" v-if="currentState">
      <div class="battle-left" v-if="selectedRole" @mouseenter="onPanelEnter" @mouseleave="onPanelLeave">
        <img :src="detailImageSrc" class="sidebar-portrait" @error="onDetailImgError" />
      </div>
      <div class="battlefield">
      <canvas ref="projectileCanvas" class="projectile-canvas"></canvas>
      <div class="player-bar player-bar-top" v-if="team1Player.name">
        <img :src="team1Player.avatar" class="player-bar-avatar" />
        <span class="player-bar-name">{{ team1Player.name }}</span>
        <div class="turn-info">
          <el-tag v-if="currentAction" type="warning" size="small">
            当前行动: {{ currentAction.team }}阵营 {{ currentAction.pos }}号位
          </el-tag>
          <el-tag v-if="gameOver" type="danger" size="small">战斗结束</el-tag>
        </div>
        <div class="ready-area" v-if="!battleStarted">
          <span class="ready-status" :class="{ ready: opponentReady }">{{ opponentReady ? '✓ 已就绪' : '未就绪' }}</span>
          <el-button class="ready-btn" :type="myReady ? 'success' : 'warning'" :disabled="myReady" @click="clickReady" size="small">
            {{ myReady ? '已就绪 ✓' : '就绪，开始战斗！' }}
          </el-button>
        </div>
      </div>

      <div class="team-section">
        <div class="role-row">
          <BattleRoleCard v-for="(role, idx) in currentState.teams[1]"
            :key="'b-' + idx" :role="role" :active="isActive(1, idx)" :effect="getEffect(1, idx)"
            :data-team="1" :data-pos="idx" @hover="selectRole(1, idx)" @leave="onCardLeave" @pin="pinRole(1, idx)" />
        </div>
      </div>

      <div class="vs-divider">VS</div>

      <div class="team-section">
        <div class="role-row">
          <BattleRoleCard v-for="(role, idx) in currentState.teams[0]"
            :key="'a-' + idx" :role="role" :active="isActive(0, idx)" :effect="getEffect(0, idx)"
            :data-team="0" :data-pos="idx" @hover="selectRole(0, idx)" @leave="onCardLeave" @pin="pinRole(0, idx)" />
        </div>
      </div>

      <div class="player-bar player-bar-bottom" v-if="team0Player.name">
        <span class="player-bar-name">{{ team0Player.name }}</span>
        <img :src="team0Player.avatar" class="player-bar-avatar" />
      </div>

      </div>
      <div class="battle-right-placeholder">
        <span class="placeholder-text"></span>
      </div>
      <div :class="['battle-right', { 'battle-right-visible': selectedRole }]" @mouseenter="onPanelEnter" @mouseleave="onPanelLeave">
        <el-button class="detail-close" @click="clearRole" circle>✕</el-button>
        <h2 class="detail-name">{{ selectedRole?.role.name }}</h2>
        <div class="detail-info" v-if="selectedRole">
          <p><span class="detail-label">HP</span> {{ selectedRole.role.hp }} / {{ selectedRole.role.maxHp }}</p>
          <p><span class="detail-label">技力</span> {{ selectedRole.role.mana }} / {{ selectedRole.role.maxMana }}</p>
          <p><span class="detail-label">攻击</span> {{ selectedRole.role.attack }}</p>
          <p><span class="detail-label">被动</span>
            <el-tag v-for="p in selectedRole.role.passives" :key="p" size="small" type="info" effect="plain" style="margin-right:4px">{{ passiveName(p) }}</el-tag>
            <span v-if="!selectedRole.role.passives.length">无</span>
          </p>
          <p><span class="detail-label">技能</span>
            <el-tag v-for="s in selectedRole.role.skills" :key="s" size="small" type="success" effect="plain" style="margin-right:4px">{{ skillName(s) }}</el-tag>
            <span v-if="!selectedRole.role.skills.length">无</span>
          </p>
          <p><span class="detail-label">状态</span>
            <el-tag :type="selectedRole.role.alive ? 'success' : 'danger'" size="small">{{ selectedRole.role.alive ? '存活' : '阵亡' }}</el-tag>
          </p>
          <p v-if="selectedRole.role.buffList.length > 0">
            <span class="detail-label">Buff</span>
            <el-tag v-for="b in selectedRole.role.buffList" :key="b.name" :type="b.type" size="small" effect="dark" style="margin-right:4px">{{ b.label }}</el-tag>
          </p>
        </div>
      </div>
    </div>

    <div class="log-float" v-if="showLog">
      <div class="log-float-header">
        <span>战斗日志</span>
        <el-button class="log-float-close" @click="toggleLog" circle size="small">✕</el-button>
      </div>
      <div class="log-float-body" ref="logContainer">
        <div v-for="(step, idx) in visibleSteps" :key="idx"
          :class="['log-line', step.type, { 'log-current': idx === currentStep }]">
          <span class="step-num">[{{ idx }}]</span>
          {{ step.text }}
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import BattleRoleCard from './BattleRoleCard.vue'
import { useBattleLog } from '../composables/useBattleLog'
import { useProjectile } from '../composables/useProjectile'
import { roleBgUrl, avatarUrl } from '../config/cdn'
import { skillName, passiveName } from '../config/skillNames'
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { gameState } from '../composables/useGameState'
import { useGameWs } from '../composables/useGameWs'

const { connect, send, onMessage } = useGameWs()
const {
  sessionId, roleInput, loading, currentStep, autoPlaying, showLog, steps,
  currentAction, gameOver, logContainer,
  visibleSteps, currentState, currentEvent,
  startBattle, loadBattle, toggleLog, stepBack, stepForward, stepTo, autoPlay, isActive, getEffect
} = useBattleLog()

const projectileCanvas = ref(null)
useProjectile(projectileCanvas, currentEvent)

const battleInner = ref(null)

const innerStyle = computed(() => ({
  width: '100%',
  height: '100%'
}))

const team1Player = computed(() => {
  if (gameState.role === 'host') {
    return { name: gameState.opponentUsername, avatar: avatarUrl(gameState.opponentAssistantId) }
  }
  return { name: gameState.myUsername, avatar: avatarUrl(gameState.myAssistantId) }
})
const team0Player = computed(() => {
  if (gameState.role === 'host') {
    return { name: gameState.myUsername, avatar: avatarUrl(gameState.myAssistantId) }
  }
  return { name: gameState.opponentUsername, avatar: avatarUrl(gameState.opponentAssistantId) }
})

watch(gameOver, (val) => {
  if (!val) return
  const evt = currentEvent.value
  if (!evt || evt.type !== 'gameover') return
  const isDraw = evt.draw
  const playerWon = isDraw ? false
    : gameState.role === 'host' ? evt.winner === 0
    : evt.winner === 1
  const title = isDraw ? '平局' : (playerWon ? '胜利！' : '失败...')
  const type = isDraw ? 'warning' : (playerWon ? 'success' : 'error')
  ElMessageBox.alert(title, '战斗结束', {
    confirmButtonText: '返回房间',
    type: type,
    center: true,
    callback: () => {
      gameState.backToLobby = true
      gameState.phase = 'none'
    }
  })
})

onMounted(() => {
  const roleIds = gameState.roleIds
  if (roleIds) {
    roleInput.value = roleIds
    startBattle()
  }
  window.addEventListener('keydown', onKeydown)
  connect(roomId.value, userId.value, mySide.value, gameState.myUsername)
})

const unregister = onMessage((msg) => {
  if (msg.type === 'PLAYER_READY') {
    if (msg.side !== mySide.value) opponentReady.value = true
  } else if (msg.type === 'BATTLE_BEGIN') {
    battleStarted.value = true
    autoPlay()
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  unregister()
})

const roomId = computed(() => gameState.roomId)
const userId = computed(() => gameState.userId)
const mySide = computed(() => gameState.mySide)
const myReady = ref(false)
const opponentReady = ref(false)
const battleStarted = ref(false)
function clickReady() {
  if (myReady.value) return
  myReady.value = true
  send({ type: 'READY', roomId: roomId.value, role: mySide.value })
  ElMessage.success('已就绪，等待对方...')
}

const selectedRole = ref(null)
const pinned = ref(false)
const showControl = ref(false)

function onKeydown(e) {
  if (e.ctrlKey && e.shiftKey && e.key === 'D') {
    e.preventDefault()
    showControl.value = !showControl.value
  }
}

function selectRole(team, pos) {
  if (!currentState.value) return
  if (pinned.value) return
  const role = currentState.value.teams[team][pos]
  if (!role) return
  if (leaveTimer) clearTimeout(leaveTimer)
  selectedRole.value = { team, pos, role }
}
function pinRole(team, pos) {
  if (!currentState.value) return
  const role = currentState.value.teams[team][pos]
  if (!role) return
  if (leaveTimer) clearTimeout(leaveTimer)
  selectedRole.value = { team, pos, role }
  pinned.value = true
}
function clearRole() {
  selectedRole.value = null
  pinned.value = false
}

const detailImageSrc = computed(() => {
  if (!selectedRole.value) return ''
  const pid = selectedRole.value.role.portraitId
  return pid ? roleBgUrl(pid) : roleBgUrl('default')
})

function onDetailImgError(e) {
  if (!e.target.src.includes('default')) {
    e.target.src = roleBgUrl('default')
  }
}

let leaveTimer = null
function onCardLeave() {
  if (pinned.value) return
  leaveTimer = setTimeout(() => {
    selectedRole.value = null
  }, 200)
}
function onPanelEnter() {
  if (leaveTimer) clearTimeout(leaveTimer)
}
function onPanelLeave() {
  if (pinned.value) return
  selectedRole.value = null
}
</script>

<style scoped>
.battle-viewer {
  width: 100%; height: 100%; overflow: hidden;
  background: #1a1a2e; color: #e0e0e0;
}
.battle-inner {
  display: flex; flex-direction: column;
  height: 100%; padding: 12px;
}
.control-card { flex-shrink: 0; margin-bottom: 8px; background: #16213e; border-color: #0f3460; }
.control-panel { flex-shrink: 0; }
.control-slide-enter-active,
.control-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.control-slide-enter-from,
.control-slide-leave-to {
  transform: translateY(-100%);
  opacity: 0;
  margin-bottom: 0;
  max-height: 0;
}
.battle-area { flex: 1; position: relative; overflow: hidden; border-radius: 8px; min-height: 0; }
.battlefield { width: 100%; height: 100%; position: relative; z-index: 1;
  display: flex; flex-direction: column; justify-content: center; }
.player-bar { display: flex; align-items: center; gap: 10px; padding: 4px 380px; position: relative; }
.player-bar-top { justify-content: flex-start; }
.player-bar-bottom { justify-content: flex-end; }
.player-bar-avatar { width: 60px; height: 60px; border: 2px solid rgba(255,255,255,0.3); object-fit: contain; background: #fff; }
.player-bar-name { color: #ffd54f; font-size: 22px; font-weight: 600; }
.ready-area { display: flex; align-items: center; gap: 12px; margin-left: auto; }
.ready-status { font-size: 14px; color: #888; }
.ready-status.ready { color: #66bb6a; }
.ready-btn { font-size: 14px; }
.projectile-canvas {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  pointer-events: none; z-index: 10;
}
.turn-info { position: absolute; left: 50%; transform: translateX(-50%); }
.turn-info :deep(.el-tag) { font-size: 24px; padding: 20px 40px; }
.team-section { margin-bottom: 8px; }
.team-title { text-align: center; margin: 8px 0; font-size: 18px; }
.team-a { color: #4fc3f7; }
.team-b { color: #ef5350; }
.role-row { display: flex; gap: 12px; justify-content: center; flex-wrap: nowrap; }
.vs-divider { text-align: center; font-size: 24px; font-weight: bold; color: #ffd54f; margin: 12px 0; }

.log-float {
  position: absolute; top: 60px; right: 16px; bottom: 16px;
  width: 420px; max-width: 45%;
  background: rgba(22, 33, 62, 0.97);
  border: 1px solid #0f3460;
  border-radius: 8px;
  display: flex; flex-direction: column;
  z-index: 100;
  box-shadow: 0 4px 24px rgba(0,0,0,0.6);
}
.log-float-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; border-bottom: 1px solid #0f3460;
  color: #ffd54f; font-size: 14px; font-weight: bold;
  flex-shrink: 0;
}
.log-float-close {
  background: transparent; border-color: #555; color: #aaa;
}
.log-float-body {
  flex: 1; overflow-y: auto; padding: 8px;
  font-family: 'Courier New', monospace; font-size: 13px;
}
.log-line { padding: 2px 8px; border-left: 3px solid transparent; }
.log-current { background: rgba(255, 213, 79, 0.15); border-left-color: #ffd54f; }
.log-line.action { color: #ffd54f; }
.log-line.attack { color: #ef5350; }
.log-line.damage { color: #ff9800; }
.log-line.hp { color: #4fc3f7; }
.log-line.death { color: #f44336; font-weight: bold; }
.log-line.passive { color: #81c784; }
.log-line.skill { color: #ba68c8; font-weight: bold; }
.log-line.add_passive { color: #66bb6a; }
.log-line.buff { color: #ce93d8; }
.log-line.swap { color: #90caf9; }
.log-line.gameover { color: #f44336; font-size: 16px; font-weight: bold; }
.step-num { color: #666; margin-right: 8px; font-size: 11px; }

.battle-left {
  position: absolute; left: 0; top: 0; height: 100%; z-index: 0;
  width: 420px;
}
.battle-right-placeholder {
  position: absolute; right: 0; top: 0; height: 100%; z-index: 4;
  width: 320px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 0 8px 8px 0;
}
.placeholder-text {
  color: rgba(255,255,255,0.3); font-size: 14px; writing-mode: vertical-rl;
  letter-spacing: 4px;
}
.battle-right {
  position: absolute; right: 0; top: 0; height: 100%; z-index: 6;
  width: 320px;
  background: rgba(30, 58, 95, 0.95);
  padding: 16px;
  border-radius: 0 8px 8px 0;
  display: flex; flex-direction: column; justify-content: flex-start;
  overflow-y: auto;
  scrollbar-width: none;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.battle-right-visible {
  transform: translateX(0);
  box-shadow: -4px 0 16px rgba(0,0,0,0.5);
}
.battle-right::-webkit-scrollbar { display: none; }
.sidebar-portrait {
  width: 100%; height: 100%; object-fit: cover; object-position: top center;
}
.detail-close {
  position: absolute; top: 16px; right: 16px;
}
.detail-name {
  font-size: 56px; color: #ffd54f; margin: 0 0 28px 0;
  text-align: center;
}
.detail-info p {
  margin: 14px 0; font-size: 32px; color: #e0e0e0;
  word-break: break-word; overflow-wrap: break-word;
}
.detail-info .el-tag {
  white-space: normal; height: auto; line-height: 1.4;
  padding: 4px 10px; margin-bottom: 4px; font-size: 24px;
  font-weight: 600; -webkit-font-smoothing: antialiased;
  color: #1a1a2e !important;
}
.detail-label {
  display: inline-block; width: 130px; font-weight: bold; color: #90caf9;
}
.battle-viewer *:focus { outline: none; }
.battle-viewer .el-card:focus-visible { outline: none; box-shadow: none; }
</style>