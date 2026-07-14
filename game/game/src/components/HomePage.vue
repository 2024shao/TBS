<template>
  <div class="home-page" @click.capture="onRootClick">
    <img v-if="!bgFailed" :src="bgSrc" class="home-bg" @error="bgFailed = true" />
  <img v-if="!assistantFailed" :src="assistantSrc" class="home-assistant" @error="onAssistantError" />
    <!-- 左上角 -->
    <div class="top-left">
      <el-button circle @click="openSettings" title="设置" class="btn-icon">
        <img src="/button/settings.svg" />
      </el-button>
      <el-button circle @click="openMail" title="邮箱" class="btn-icon">
        <img src="/button/email.svg" />
      </el-button>
    </div>

    <!-- 左中间 -->
    <div class="mid-left">
      <span class="username">{{ username }}</span>
      <el-button circle class="btn-refresh" @click="refreshBg" title="更换背景">
        <img src="/button/Home_page_switching.svg" class="btn-svg" />
      </el-button>
    </div>

    <!-- 右边按钮 -->
    <div class="right-btns">
      <!-- 启动按钮 -->
      <div class="svg-btn btn-start-svg" @click="startGame" style="background-image: url('/button/start.svg')">
        <span class="btn-label"><img src="/log/start-log.svg" /></span>
        <div class="btn-quick-link" @click.stop="quickMatch">
          <img src="/button/quick-link.svg" />
        </div>
        <div class="btn-create-room" @click.stop="openRoom">
          <img src="/button/create-room.svg" />
        </div>
      </div>

      <!-- 记录按钮 -->
      <div class="svg-btn btn-records" @click="openRecords" style="background-image: url('/button/record.svg')">
        <span class="btn-label"><img src="/log/record-log.svg" /></span>
      </div>

      <!-- 角色按钮 -->
      <div class="svg-btn btn-roles" @click="openRoles" style="background-image: url('/button/roles.svg')">
        <span class="btn-label"><img src="/log/roles-log.svg" /></span>
      </div>

      <!-- 好友按钮 -->
      <div class="svg-btn btn-friends" @click="openFriends" style="background-image: url('/button/friends.svg')">
        <span class="btn-label"><img src="/log/friends-log.svg" /></span>
      </div>

      <!-- 收藏按钮 -->
      <div class="svg-btn btn-favorites" @click="openFavorites" style="background-image: url('/button/favorites.svg')">
        <span class="btn-label"><img src="/log/favorites-log.svg" /></span>
      </div>
    </div>
    <!-- 上窗口：自上而下滑入，返回按钮 -->
    <div :class="['top-slide-panel', { 'top-show': showPanel }]">
      <el-button class="btn-back" @click="closePanel">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
    </div>

    <!-- 场景选择面板：自右向左滑入 -->
    <div :class="['scene-slide-panel', { 'scene-show': showScenePanel }]">
      <div class="scene-panel-top">
        <el-button class="btn-back" @click="openScene">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="scene-panel-title">更换场景</span>
      </div>
      <div class="scene-list">
        <div
          v-for="bg in bgList" :key="bg"
          :class="['scene-thumb', { 'scene-selected': selectedBg === bg }]"
          @click="previewBg(bg)"
        >
          <img :src="bgUrl(bg)" />
        </div>
      </div>
      <el-button class="btn-confirm" @click="confirmBg">确认</el-button>
    </div>

    <!-- 下窗口：自下而上滑入，功能按钮 -->
    <div :class="['bottom-slide-panel', { 'bottom-show': showPanel }]">
      <div class="bottom-actions">
        <div class="action-item" @click="openAssistant">
          <el-icon><Edit /></el-icon>
          <span>编辑助理</span>
        </div>
        <div class="action-divider"></div>
        <div class="action-item" @click="openTheme">
          <el-icon><MagicStick /></el-icon>
          <span>更换主题</span>
        </div>
        <div class="action-divider"></div>
        <div class="action-item" @click="openScene">
          <el-icon><Picture /></el-icon>
          <span>更换场景</span>
        </div>
      </div>
    </div>

    <!-- 设置面板 -->
    <div :class="['settings-panel', { 'settings-show': showSettings }]" @click.self="showSettings = false">
      <div class="settings-top">
        <el-button class="btn-back" @click="showSettings = false">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="settings-title">设置</span>
      </div>
      <div class="settings-body">
        <div class="settings-tabs">
          <div
            v-for="tab in settingsTabs" :key="tab.key"
            :class="['settings-tab', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >{{ tab.label }}</div>
        </div>
        <div class="settings-content">
          <div v-if="activeTab === 'game'" class="tab-panel">
            <div class="setting-row">
              <span>全屏模式</span>
              <el-switch v-model="isFullscreen" @change="toggleFullscreen" />
            </div>
            <div class="setting-row">
              <div>
                <span>游戏速度倍数</span>
                <p class="setting-hint">*战斗时执行动画的速度</p>
              </div>
              <el-select v-model="gameSpeed" @change="saveSpeed" style="width: 100px;">
                <el-option label="1 倍速" :value="1" />
                <el-option label="2 倍速" :value="2" />
              </el-select>
            </div>
          </div>
          <div v-if="activeTab === 'sound'" class="tab-panel">
            <div class="setting-row">
              <span>音乐音量</span>
              <div class="sound-right">
                <el-switch v-model="musicMuted" @change="saveVolume" size="small" />
                <el-slider v-model="musicVolume" :min="0" :max="100" :step="1" :disabled="musicMuted" style="width: 150px" @change="saveVolume" />
              </div>
            </div>
            <div class="setting-row">
              <span>音效音量</span>
              <div class="sound-right">
                <el-switch v-model="effectMuted" @change="saveVolume" size="small" />
                <el-slider v-model="effectVolume" :min="0" :max="100" :step="1" :disabled="effectMuted" style="width: 150px" @change="saveVolume" />
              </div>
            </div>
            <div class="setting-row">
              <div>
                <span>语音音量</span>
                <p class="setting-hint">*角色语音播放音量</p>
              </div>
              <div class="sound-right">
                <el-switch v-model="voiceMuted" @change="saveVolume" size="small" />
                <el-slider v-model="voiceVolume" :min="0" :max="100" :step="1" :disabled="voiceMuted" style="width: 150px" @change="saveVolume" />
              </div>
            </div>
            <div class="setting-row">
              <div>
                <span>语音语言</span>
                <p class="setting-hint">*角色语音播放语言</p>
              </div>
              <el-select v-model="voiceLang" @change="saveVoiceLang" style="width: 130px;">
                <el-option label="中文-普通话" value="zh" />
                <el-option label="日语" value="ja" />
                <el-option label="英语" value="en" />
              </el-select>
            </div>
          </div>
          <div v-if="activeTab === 'user'" class="tab-panel">
            <div class="user-actions">
              <el-button class="btn-user" @click="goAccount">账号管理</el-button>
              <el-button class="btn-user" @click="goAbout">关于游戏</el-button>
              <el-button class="btn-user btn-logout" @click="logout">退出登录</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 启动面板（自右向左滑入） -->
    <div :class="['launch-panel', { 'launch-show': showLaunchPanel }]">
      <div class="launch-header">
        <el-button class="btn-back" @click="showLaunchPanel = false">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="launch-title">启动</span>
      </div>
      <div class="launch-body">
        <div class="launch-row">
          <el-button class="launch-btn" @click="quickMatch">
            <el-icon><Connection /></el-icon>
            快速连接
          </el-button>
        </div>
        <div class="launch-row">
          <el-button class="launch-btn" @click="openRoom">
            <el-icon><HomeFilled /></el-icon>
            创建房间
          </el-button>
        </div>
        <div class="launch-row">
          <el-button class="launch-btn" @click="crisisContract">
            <el-icon><WarningFilled /></el-icon>
            合约??
          </el-button>
        </div>
      </div>
    </div>

    <!-- 连接状态提示 -->
    <div v-if="isMatching" class="match-overlay">
      <div class="match-box">
        <span class="match-loading"></span>
        <span>{{ matchStatus }}</span>
        <el-button size="small" @click="cancelQuickMatch">取消匹配</el-button>
      </div>
    </div>

    <!-- 创建房间面板 -->
    <div :class="['room-panel', { 'room-show': showRoomPanel }]">
      <div class="launch-header">
        <el-button class="btn-back" @click="doLeave">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="launch-title">{{ roomRole === 'guest' ? '对战房间' : '创建房间' }}</span>
      </div>
      <div class="room-body">
        <!-- 房主视图 -->
        <template v-if="roomRole === 'host'">
          <div class="room-slots">
            <div v-if="mySlot === 0" :class="['room-slot', { 'slot-ready': hostReady }]">
              <div class="slot-label">0号位</div>
              <img :src="avatarUrl(userAssistantId)" class="slot-avatar" />
              <span class="slot-name">{{ username }}（房主）</span>
              <el-button
                size="small"
                :class="['btn-ready', { 'btn-ready-done': hostReady }]"
                :disabled="!guestInfo.id"
                @click="toggleHostReady"
              >{{ hostReady ? '已准备' : '准备' }}</el-button>
            </div>
            <div v-else-if="guestInfo.id" :class="['room-slot', { 'slot-ready': guestReady }]">
              <div class="slot-label">0号位</div>
              <img :src="avatarUrl(guestInfo.assistantId)" class="slot-avatar" />
              <span class="slot-name">{{ guestInfo.username }}</span>
              <span class="btn-ready-placeholder"></span>
            </div>
            <div v-else class="room-slot empty-slot">
              <div class="slot-label">0号位</div>
              <div class="slot-avatar empty">+</div>
              <span class="slot-name">空位</span>
              <span class="btn-ready-placeholder"></span>
            </div>
            <div v-if="mySlot === 1" :class="['room-slot', { 'slot-ready': hostReady }]">
              <div class="slot-label">1号位</div>
              <img :src="avatarUrl(userAssistantId)" class="slot-avatar" />
              <span class="slot-name">{{ username }}（房主）</span>
              <el-button
                size="small"
                :class="['btn-ready', { 'btn-ready-done': hostReady }]"
                :disabled="!guestInfo.id"
                @click="toggleHostReady"
              >{{ hostReady ? '已准备' : '准备' }}</el-button>
            </div>
            <div v-else-if="guestInfo.id" :class="['room-slot', { 'slot-ready': guestReady }]">
              <div class="slot-label">1号位</div>
              <img :src="avatarUrl(guestInfo.assistantId)" class="slot-avatar" />
              <span class="slot-name">{{ guestInfo.username }}</span>
              <span class="btn-ready-placeholder"></span>
            </div>
            <div v-else class="room-slot empty-slot">
              <div class="slot-label">1号位</div>
              <div class="slot-avatar empty">+</div>
              <span class="slot-name">空位</span>
              <span class="btn-ready-placeholder"></span>
            </div>
          </div>
          <el-button
            class="btn-start-sim"
            :class="{ 'btn-start-sim-ready': allReady }"
            :disabled="!allReady"
            @click="startSimulation"
          >开始模拟</el-button>
          <el-button class="btn-leave" @click="doLeave">离开房间</el-button>
          <div class="room-friends">
            <h4>好友列表</h4>
            <div v-if="friendList.length === 0" class="empty">暂无好友</div>
            <div v-for="f in friendList" :key="f.friendId" class="friend-item">
              <img :src="avatarUrl(f.assistantId)" class="search-avatar" />
              <div class="search-info">
                <span class="search-name">{{ f.username }}</span>
              </div>
              <el-button size="small" type="primary" :disabled="!!guestInfo.id" @click="doInvite(f)">邀请</el-button>
            </div>
          </div>
        </template>
        <!-- 被邀请方视图 -->
        <template v-if="roomRole === 'guest'">
          <div class="room-slots">
            <div :class="['room-slot', { 'slot-ready': hostReady }]">
              <div class="slot-label">0号位</div>
              <img :src="avatarUrl(hostInfo.assistantId)" class="slot-avatar" />
              <span class="slot-name">{{ hostInfo.username }}（房主）</span>
              <span class="btn-ready-placeholder"></span>
            </div>
            <div :class="['room-slot', { 'slot-ready': guestReady }]">
              <div class="slot-label">1号位</div>
              <img :src="avatarUrl(userAssistantId)" class="slot-avatar" />
              <span class="slot-name">{{ username }}</span>
              <el-button
                size="small"
                :class="['btn-ready', { 'btn-ready-done': guestReady }]"
                @click="toggleGuestReady"
              >{{ guestReady ? '已准备' : '准备' }}</el-button>
            </div>
          </div>
          <el-button class="btn-start-sim" disabled>开始模拟</el-button>
          <el-button class="btn-leave" @click="doLeave">离开房间</el-button>
          <div class="room-friends">
            <p class="room-waiting">等待房主开始游戏...</p>
          </div>
        </template>
      </div>
    </div>

    <!-- 好友面板（自右向左滑入，50vw） -->
    <div :class="['friends-panel', { 'friends-panel-show': showFriendsPanel }]">
      <div class="friends-panel-top">
        <el-button class="btn-back" @click="showFriendsPanel = false; friendsActiveTab = 'profile'">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="friends-panel-title">好友</span>
      </div>
      <div class="friends-panel-body">
        <div class="friends-panel-tabs">
          <div
            v-for="tab in friendsTabs" :key="tab.key"
            :class="['friends-panel-tab', { active: friendsActiveTab === tab.key }]"
            @click="switchFriendsTab(tab.key)"
          >{{ tab.label }}</div>
        </div>
        <div class="friends-panel-content">
          <!-- 个人名片：提前加载 -->
          <div v-show="friendsActiveTab === 'profile'" class="tab-panel">
            <div class="profile-card">
              <img :src="avatarUrl(userAssistantId)" class="profile-avatar" />
              <h3>{{ username }}</h3>
              <p>ID: {{ userId }}</p>
            </div>
          </div>
          <!-- 好友列表：按需加载 -->
          <div v-if="friendsActiveTab === 'list'" class="tab-panel">
            <div v-if="friendRequests.length > 0" class="section">
              <h4>好友申请 ({{ friendRequests.length }})</h4>
              <div v-for="r in friendRequests" :key="r.id" class="friend-item">
                <img :src="avatarUrl(r.assistantId)" class="search-avatar" />
                <div class="search-info">
                  <span class="search-name">{{ r.username }}</span>
                </div>
                <div class="item-actions">
                  <el-button size="small" @click="doFriendAccept(r)">同意</el-button>
                  <el-button size="small" @click="doFriendReject(r)">拒绝</el-button>
                </div>
              </div>
            </div>
            <div class="section">
              <h4>我的好友 ({{ friendList.length }})</h4>
              <div v-if="friendList.length === 0" class="empty">暂无好友</div>
              <div v-for="f in friendList" :key="f.friendId" class="friend-item">
                <img :src="avatarUrl(f.assistantId)" class="search-avatar" />
                <div class="search-info">
                  <span class="search-name">{{ f.username }}</span>
                </div>
                <span class="friend-time">{{ formatFriendTime(f.lastLoginAt) }}</span>
                <el-button size="small" @click="doFriendDelete(f)">删除</el-button>
              </div>
            </div>
          </div>
          <!-- 添加好友：按需加载 -->
          <div v-if="friendsActiveTab === 'add'" class="tab-panel">
            <div class="search-box">
              <el-input v-model="friendKeyword" placeholder="搜索玩家用户名" @keyup.enter="doFriendSearch" />
              <el-button @click="doFriendSearch">搜索</el-button>
            </div>
            <div v-if="friendSearchResults.length > 0" class="section">
              <div v-for="u in friendSearchResults" :key="u.id" class="friend-item">
                <img :src="avatarUrl(u.assistantId)" class="search-avatar" />
                <div class="search-info">
                  <span class="search-name">{{ u.username }}</span>
                  <span class="search-id">ID: {{ u.id }}</span>
                </div>
                <el-button v-if="u.relation === 'friend'" size="small" disabled>已是好友</el-button>
                <el-button v-else-if="u.relation === 'pending'" size="small" disabled>已申请</el-button>
                <el-button v-else size="small" type="primary" @click.stop="doFriendAdd(u)">添加好友</el-button>
              </div>
            </div>
            <div v-if="friendSearched && friendSearchResults.length === 0" class="empty">未找到玩家</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 邀请弹窗 -->
    <div v-if="showInvitePopup" class="invite-overlay" @click.self="showInvitePopup = false">
      <div class="invite-popup">
        <h3>收到邀请</h3>
        <img :src="avatarUrl(inviteData.assistantId)" class="invite-avatar" />
        <p>{{ inviteData.fromUsername }} 邀请你对战</p>
        <div class="invite-actions">
          <el-button type="primary" @click="doInviteAccept">接受</el-button>
          <el-button @click="doInviteReject">拒绝</el-button>
        </div>
      </div>
    </div>

    <!-- 助理面板：自右向左滑入 -->
    <div :class="['assistant-panel', { 'assistant-panel-show': showAssistantPanel }]">
      <div class="assistant-panel-top">
        <el-button class="btn-back" @click="cancelAssistant">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="assistant-panel-title">编辑助理</span>
      </div>
      <div class="assistant-panel-body">
        <div class="assistant-grid">
          <div
            v-for="id in avatarList"
            :key="id"
            :class="['assistant-item', { 'assistant-selected': previewAssistantId === id }]"
            @click="selectAssistant(id)"
          >
            <img :src="avatarUrl(id)" class="assistant-avatar" />
          </div>
        </div>
      </div>
      <div class="assistant-panel-bottom">
        <el-button class="btn-confirm" @click="confirmAssistant">确认</el-button>
      </div>
    </div>

    <!-- 主题面板：自右向左滑入 -->
    <div :class="['theme-panel', { 'theme-panel-show': showThemePanel }]">
      <div class="theme-panel-top">
        <el-button class="btn-back" @click="cancelTheme">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="theme-panel-title">更换主题</span>
      </div>
      <div class="theme-panel-body">
        <div class="theme-list">
          <div
            v-for="theme in themeList" :key="theme"
            :class="['theme-item', { 'theme-selected': selectedTheme === theme }]"
            @click="previewTheme(theme)"
          >
            <span class="theme-item-text">主题 {{ theme }}</span>
          </div>
        </div>
      </div>
      <el-button class="btn-confirm" @click="confirmTheme">确认</el-button>
    </div>

    <!-- 角色面板：自右向左滑入 -->
    <div :class="['role-panel', { 'role-panel-show': showRolePanel }]">
      <div class="role-panel-top">
        <el-button class="btn-back" @click="showRolePanel = false">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="role-panel-title">角色</span>
      </div>
      <div class="role-panel-body">
        <div class="role-detail">
          <div class="role-detail-left">
            <img v-if="selectedRole" :src="roleBgUrl(selectedRole.portraitId)" class="role-portrait" @error="onRolePortraitError" />
            <div v-else class="role-portrait-empty">点击角色头像查看详情</div>
          </div>
          <div class="role-detail-right">
            <template v-if="selectedRole">
              <h3>{{ selectedRole.roleName }}</h3>
              <div class="role-stats">
                <p>生命值：{{ selectedRole.hp }} / {{ selectedRole.hpMax }}</p>
                <p>攻击力：{{ selectedRole.attack }}</p>
              </div>
              <div class="role-skill-desc">
                <p><strong>技能：</strong>{{ selectedRole.skillDescription || '无' }}</p>
              </div>
              <div class="role-passive-desc">
                <p><strong>被动：</strong>{{ selectedRole.passiveDescription || '无' }}</p>
              </div>
              <div v-if="selectedRole.remark" class="role-remark-text">
                <p>{{ selectedRole.remark }}</p>
              </div>
            </template>
            <div v-else class="role-detail-empty">请选择一个角色</div>
          </div>
        </div>
        <div class="role-list">
          <div
            v-for="role in roleList"
            :key="role.roleId"
            :class="['role-list-item', { 'role-list-item-selected': selectedRole && selectedRole.roleId === role.roleId }]"
            @click="selectRole(role)"
          >
            <img :src="avatarUrl(role.portraitId)" class="role-list-icon" @error="onRoleListImgError" />
            <span class="role-list-name">{{ role.roleName }}</span>
          </div>
        </div>
      </div>
    </div>

  </div>
  <Transition name="fade">
    <div class="game-overlay" v-if="gameState.phase !== 'none'">
      <Transition name="fade">
        <PickPhase v-if="gameState.phase === 'pick'" key="pick" />
        <PositionAdjust v-else-if="gameState.phase === 'position'" key="position" />
        <BattleLogViewer v-else-if="gameState.phase === 'game'" key="game" />
      </Transition>
    </div>
  </Transition>
  <div v-if="isTransitioning" class="transition-blackout" @animationend="isTransitioning = false"></div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { UserFilled, ArrowLeft, Edit, MagicStick, Picture, VideoPlay, TrophyBase, Avatar, Star, Connection, HomeFilled, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { updateBackground, updateAssistant, updateBackgroundMusic } from '../api/auth'
import { searchUser, addFriend, acceptFriend, rejectFriend, deleteFriend, getFriendList, getFriendRequests } from '../api/friend'
import { inviteFriend, getPendingInvitations, acceptInvitation, rejectInvitation, leaveRoom } from '../api/room'
import { getAllRoles } from '../api/game'
import PickPhase from './PickPhase.vue'
import PositionAdjust from './PositionAdjust.vue'
import BattleLogViewer from './BattleLogViewer.vue'
import { gameState, isTransitioning, doPhaseTransition } from '../composables/useGameState'
import { avatarUrl, bgUrl, roleBgUrl, homepageBgmUrl } from '../config/cdn'
import { useClickSound } from '../composables/useClickSound'
import { useGameWs } from '../composables/useGameWs'
import { joinMatch, cancelMatch, checkMatchStatus } from '../api/match'

const { playClick, setVolume: setEffectVolume } = useClickSound()

function onRootClick(e) {
  if (bgmAudio.value && bgmAudio.value.paused) {
    bgmAudio.value.play().catch(() => {})
  }
  playClick()
}

const ASSISTANT_IDS = [
  30086001, 30086002, 30086003, 30086004, 30086005, 30086006, 30086007, 30086008,
  30086009, 30086010, 30086011, 30086012, 30086013, 30086014, 30086015, 30086016,
  30086017, 30086018, 30086019, 30086020, 30086021, 30086022, 30086023, 30086024,
  30086025, 30086026
]

const router = useRouter()
const route = useRoute()
const username = ref('')
const bgSrc = ref(bgUrl('background.webp'))
const bgFailed = ref(false)
const assistantSrc = ref(roleBgUrl(30086009))
const assistantFailed = ref(false)
const bgmAudio = ref(null)
const assistantTriedFallback = ref(false)

function onAssistantError() {
  if (!assistantTriedFallback.value) {
    assistantTriedFallback.value = true
    assistantSrc.value = roleBgUrl(30086009)
  } else {
    assistantFailed.value = true
  }
}

async function doInvite(f) {
  const res = await inviteFriend(userId.value, f.friendId)
  if (res.success) {
    currentRoomId.value = res.roomId
    connectRoomWs()
  }
  ElMessage[res.success ? 'success' : 'warning'](res.message)
}

//webSocket连接
const { ws, connect, send, disconnect, onMessage } = useGameWs()

// 连接房间
function connectRoomWs() {
  connect(currentRoomId.value, userId.value, roomRole.value === 'host' ? 0 : 1, username.value, userAssistantId.value, 'lobby')
}
// 关闭房间（替代 closeRoomWs）
function closeRoomWs() {
  disconnect()
}


const currentRoomId = ref(null)
const guestInfo = ref({})

async function toggleHostReady() {
  hostReady.value = !hostReady.value
  send({ type: 'LOBBY_READY', roomId: currentRoomId.value, side: 0, ready: hostReady.value })
}

async function toggleGuestReady() {
  guestReady.value = !guestReady.value
  send({ type: 'LOBBY_READY', roomId: currentRoomId.value, side: 1, ready: guestReady.value })
}

// 邀请弹窗
const showInvitePopup = ref(false)
const inviteData = ref({})
let invitePollTimer = null

function startInvitePoll() {
  if (invitePollTimer) return
  invitePollTimer = setInterval(async () => {
    const res = await getPendingInvitations(userId.value)
    if (res.success && res.invitations.length > 0) {
      inviteData.value = res.invitations[0]
      showInvitePopup.value = true
    }
  }, 3000)
}

async function doInviteAccept() {
  if (currentRoomId.value) {
    closeRoomWs()
    await leaveRoom(currentRoomId.value, userId.value)
  }
  const res = await acceptInvitation(inviteData.value.id, userId.value)
  if (res.success) {
    showInvitePopup.value = false
    ElMessage.success('已接受邀请，进入房间')
    roomRole.value = 'guest'
    hostInfo.value = {
      username: inviteData.value.fromUsername,
      assistantId: inviteData.value.assistantId || 30086009
    }
    currentRoomId.value = inviteData.value.roomId
    showRoomPanel.value = true
    connectRoomWs()
  } else {
    ElMessage.warning(res.message)
    showInvitePopup.value = false
  }
}

async function doInviteReject() {
  await rejectInvitation(inviteData.value.id, userId.value)
  showInvitePopup.value = false
}

onMounted(() => {
  startInvitePoll()
  const user = localStorage.getItem('user')
  if (user) {
    const parsed = JSON.parse(user)
    username.value = parsed.username
    userId.value = parsed.id
    userAssistantId.value = parsed.assistantId || 30086009
    if (parsed.backgroundId) {
      bgSrc.value = bgUrl(`${parsed.backgroundId}.webp`)
    }
    if (parsed.assistantId) {
      assistantSrc.value = roleBgUrl(parsed.assistantId)
    }
    const bgmFile = parsed.background_music || '1'
    bgmAudio.value = new Audio(homepageBgmUrl(bgmFile))
    bgmAudio.value.loop = true
    applyMusicVolume()
    bgmAudio.value.play().catch(() => {})
  }
  setEffectVolume(effectMuted.value ? 0 : effectVolume.value / 100)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  isFullscreen.value = !!document.fullscreenElement
  window.addEventListener('beforeunload', handleBeforeUnload)

  if (route.query.tab === 'user') {
    showSettings.value = true
    activeTab.value = 'user'
  }
  onMessage((msg) => {
    switch (msg.type) {
      case 'ROOM_JOINED':
        if (roomRole.value === 'host') {
          guestInfo.value = { id: msg.guestUserId, username: msg.guestUsername, assistantId: msg.guestAssistantId }
        } else {
          hostInfo.value = { id: msg.hostUserId, username: msg.hostUsername, assistantId: msg.hostAssistantId }
        }
        break
      case 'LOBBY_READY':
        if (msg.side === 0) hostReady.value = msg.ready
        else guestReady.value = msg.ready
        break
      case 'PLAYER_LEFT':
        ElMessage.warning('对方已离开房间')
        resetRoom()
        break
      case 'BATTLE_START':
        Object.assign(gameState, {
          roomId: currentRoomId.value, userId: userId.value, role: roomRole.value,
          myUsername: username.value, myAssistantId: userAssistantId.value,
          opponentUsername: roomRole.value === 'host' ? guestInfo.value.username : hostInfo.value.username,
          opponentAssistantId: roomRole.value === 'host' ? guestInfo.value.assistantId : hostInfo.value.assistantId,
          mySide: roomRole.value === 'host' ? 0 : 1
        })
        doPhaseTransition('pick')
        break
      case 'BACK_TO_LOBBY':
        hostReady.value = false
        guestReady.value = false
        if (roomRole.value === 'host') {
          guestInfo.value = { id: msg.guestUserId, username: msg.guestUsername, assistantId: msg.guestAssistantId }
        } else {
          hostInfo.value = { id: msg.hostUserId, username: msg.hostUsername, assistantId: msg.hostAssistantId }
        }
        showRoomPanel.value = true
        break
      case 'PLAYER_RECONNECTED':
        if (msg.userId !== userId.value) {
          ElMessage.success('对方已重连')
        } else {
          ElMessage.success('重连成功')
        }
        break
    }
  })
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  if (bgmAudio.value) {
    bgmAudio.value.pause()
    bgmAudio.value = null
  }
  if (currentRoomId.value && !isStartingGame.value) {
    send({ type: 'LEAVE_ROOM', roomId: currentRoomId.value, userId: userId.value })
    closeRoomWs()
    leaveRoom(currentRoomId.value, userId.value)
  }
  resetRoom()
})

watch(() => gameState.backToLobby, (val) => {
  if (val && currentRoomId.value) {
    gameState.backToLobby = false
    hostReady.value = false
    guestReady.value = false
    showRoomPanel.value = true
    connectRoomWs()
  }
})

const showLaunchPanel = ref(false)
const showRoomPanel = ref(false)
const mySlot = ref(0)
const roomRole = ref('host')
const hostInfo = ref({})
const hostReady = ref(false)
const guestReady = ref(false)
const allReady = computed(() => hostReady.value && guestReady.value)

// function switchSlot() {
//   mySlot.value = mySlot.value === 0 ? 1 : 0
// }

function startGame() {
  showLaunchPanel.value = true
}

function openRoom() {
  showRoomPanel.value = true
  roomRole.value = 'host'
  mySlot.value = 0
  guestInfo.value = {}
  currentRoomId.value = null
  hostReady.value = false
  guestReady.value = false
  if (!friendDataLoaded.value) loadFriendData()
}
//快速连接功能
let matchPollTimer = null
const isMatching = ref(false)
const matchStatus = ref('')

async function quickMatch() {
  if (isMatching.value) return
  isMatching.value = true
  matchStatus.value = '正在连接中...'

  const res = await joinMatch(userId.value, username.value, userAssistantId.value)

  if (res.matched) {
    matchStatus.value = '连接成功！'
    ElMessage.success('连接成功！')
    enterMatchedRoom(res)
  } else {
    ElMessage.info('已加入连接队列，等待对手...')
    // 轮询检查连接状态
    matchPollTimer = setInterval(async () => {
      const status = await checkMatchStatus(userId.value)
      console.log('轮询连接状态:', status)  
      if (status.matched) {
        matchStatus.value = '连接成功！'
        ElMessage.success('连接成功！')
        enterMatchedRoom(status)
      }
    }, 2000)
  }
}

function cancelQuickMatch() {
  if (matchPollTimer) {
    clearInterval(matchPollTimer)
    matchPollTimer = null
  }
  cancelMatch(userId.value)
  isMatching.value = false
  matchStatus.value = ''
  ElMessage.info('已取消连接')
}

function enterMatchedRoom(res) {
  isMatching.value = false
  matchStatus.value = ''
  if (matchPollTimer) {
    clearInterval(matchPollTimer)
    matchPollTimer = null
  }
  // 如果已经在房间里，先退出
  if (currentRoomId.value) {
    closeRoomWs()
    leaveRoom(currentRoomId.value, userId.value)
  }
  currentRoomId.value = res.roomId
  roomRole.value = res.role
  if (res.role === 'host') {
    guestInfo.value = {
      id: res.opponentUserId,
      username: res.opponentUsername,
      assistantId: res.opponentAssistantId
    }
  } else {
    hostInfo.value = {
      id: res.opponentUserId,
      username: res.opponentUsername,
      assistantId: res.opponentAssistantId
    }
  }
  showRoomPanel.value = true
  connectRoomWs()
}

const isStartingGame = ref(false)

async function startSimulation() {
    send({ type: 'START_GAME', roomId: currentRoomId.value })
}

function handleBeforeUnload() {
  if (currentRoomId.value) {
    send({ type: 'LEAVE_ROOM', roomId: currentRoomId.value, userId: userId.value })
  }
  closeRoomWs()
}

async function doLeave() {
    send({ type: 'LEAVE_ROOM', roomId: currentRoomId.value, userId: userId.value })
  closeRoomWs()
  if (currentRoomId.value) {
    await leaveRoom(currentRoomId.value, userId.value)
  }
  resetRoom()
}

function resetRoom() {
  closeRoomWs()
  showRoomPanel.value = false
  roomRole.value = 'host'
  guestInfo.value = {}
  currentRoomId.value = null
  hostReady.value = false
  guestReady.value = false
}

function crisisContract() {
  ElMessage.info('危机合约功能开发中')
}
function openSettings() {
  showSettings.value = !showSettings.value
}

function openMail() {
  ElMessage.info('邮箱功能开发中')
}

function openFriends() {
  showFriendsPanel.value = true
  loadFriendData()
}

function openRecords() {
  ElMessage.info('战绩功能开发中')
}

async function openRoles() {
  showRolePanel.value = true
  if (roleList.value.length === 0) {
    try {
      const res = await getAllRoles()
      roleList.value = res.data || res
    } catch (e) {
      console.error('加载角色列表失败', e)
    }
  }
}

function selectRole(role) {
  selectedRole.value = role
}

function onRolePortraitError(e) {
  e.target.style.display = 'none'
}

function onRoleListImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23333" width="100" height="100"/><text fill="%23888" x="50" y="55" text-anchor="middle" font-size="12">无图片</text></svg>'
}

function openFavorites() {
  ElMessage.info('收藏功能开发中')
}

// ========== 好友面板 ==========
const showFriendsPanel = ref(false)
const showAssistantPanel = ref(false)
const previewAssistantId = ref(30086009)
const avatarList = ref([])
const showRolePanel = ref(false)
const roleList = ref([])
const selectedRole = ref(null)
const friendsActiveTab = ref('profile')
const userId = ref(null)
const userAssistantId = ref(30086009)

const friendsTabs = [
  { key: 'profile', label: '个人名片' },
  { key: 'list', label: '好友列表' },
  { key: 'add', label: '添加好友' }
]
const friendList = ref([])
const friendRequests = ref([])
const friendKeyword = ref('')
const friendSearchResults = ref([])
const friendSearched = ref(false)
const friendDataLoaded = ref(false)

function switchFriendsTab(key) {
  friendsActiveTab.value = key
  if (!friendDataLoaded.value) loadFriendData()
}

async function loadFriendData() {
  if (friendDataLoaded.value) return
  friendDataLoaded.value = true
  const [listRes, reqRes] = await Promise.all([
    getFriendList(userId.value),
    getFriendRequests(userId.value)
  ])
  if (listRes.success) friendList.value = listRes.friends
  if (reqRes.success) friendRequests.value = reqRes.requests
}

async function doFriendSearch() {
  if (!friendKeyword.value.trim()) return
  const res = await searchUser(friendKeyword.value.trim(), userId.value)
  friendSearched.value = true
  if (res.success) friendSearchResults.value = res.users
}

function formatFriendTime(time) {
  if (!time) return '从未登录'
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚在线'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString()
}

async function doFriendAdd(u) {
  const res = await addFriend({ userId: userId.value, friendId: u.id })
  ElMessage[res.success ? 'success' : 'warning'](res.message)
}

async function doFriendAccept(r) {
  const res = await acceptFriend({ userId: r.userId, friendId: userId.value })
  if (res.success) {
    ElMessage.success(res.message)
    friendDataLoaded.value = false
    loadFriendData()
  }
}

async function doFriendReject(r) {
  const res = await rejectFriend({ userId: r.userId, friendId: userId.value })
  if (res.success) {
    ElMessage.success(res.message)
    friendDataLoaded.value = false
    loadFriendData()
  }
}

async function doFriendDelete(f) {
  const res = await deleteFriend({ userId: userId.value, friendId: f.friendId })
  if (res.success) {
    ElMessage.success(res.message)
    friendDataLoaded.value = false
    loadFriendData()
  }
}

const showPanel = ref(false)
const showScenePanel = ref(false)
const showSettings = ref(false)
const bgList = ref(['background.webp', 'background1.webp', 'background2.webp'])
const selectedBg = ref('')
const originalBg = ref('')
const showThemePanel = ref(false)
const themeList = ref(['1', '2'])
const selectedTheme = ref('')
const originalTheme = ref('')

const settingsTabs = [
  { key: 'game', label: '游戏' },
  { key: 'sound', label: '声音' },
  { key: 'user', label: '用户中心' }
]
const activeTab = ref('game')
const isFullscreen = ref(false)
const gameSpeed = ref(parseInt(localStorage.getItem('gameSpeed') || '1'))

function saveSpeed() {
  localStorage.setItem('gameSpeed', gameSpeed.value)
}

const musicVolume = ref(parseInt(localStorage.getItem('musicVolume') || '80'))
const effectVolume = ref(parseInt(localStorage.getItem('effectVolume') || '80'))
const voiceVolume = ref(parseInt(localStorage.getItem('voiceVolume') || '80'))
const musicMuted = ref(localStorage.getItem('musicMuted') === 'true')
const effectMuted = ref(localStorage.getItem('effectMuted') === 'true')
const voiceMuted = ref(localStorage.getItem('voiceMuted') === 'true')
const voiceLang = ref(localStorage.getItem('voiceLang') || 'zh')

function saveVolume() {
  localStorage.setItem('musicVolume', musicVolume.value)
  localStorage.setItem('effectVolume', effectVolume.value)
  localStorage.setItem('voiceVolume', voiceVolume.value)
  localStorage.setItem('musicMuted', musicMuted.value)
  localStorage.setItem('effectMuted', effectMuted.value)
  localStorage.setItem('voiceMuted', voiceMuted.value)
  applyMusicVolume()
  setEffectVolume(effectMuted.value ? 0 : effectVolume.value / 100)
}

function applyMusicVolume() {
  if (bgmAudio.value) {
    bgmAudio.value.volume = musicMuted.value ? 0 : musicVolume.value / 100
  }
}

function saveVoiceLang() {
  localStorage.setItem('voiceLang', voiceLang.value)
}

function goAccount() {
  router.push('/account')
}

function goAbout() {
  ElMessage.info('关于游戏开发中')
}

function logout() {
  ElMessageBox.confirm('确定要退出登录吗？', '系统提示', {
    confirmButtonText: '是',
    cancelButtonText: '否',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    sessionStorage.clear()
    window.location.href = '/login'
  }).catch(() => {})
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

async function refreshBg() {
  showPanel.value = !showPanel.value
  showScenePanel.value = false
  showThemePanel.value = false
  preloadAvatars()
}

function closePanel() {
  if (showScenePanel.value) {
    bgSrc.value = originalBg.value
    bgFailed.value = false
  }
  if (showThemePanel.value && bgmAudio.value) {
    cancelTheme()
  }
  showPanel.value = false
  showScenePanel.value = false
  showThemePanel.value = false
}

function openAssistant() {
  previewAssistantId.value = userAssistantId.value
  showAssistantPanel.value = true
}

function preloadAvatars() {
  avatarList.value = ASSISTANT_IDS
}

function selectAssistant(id) {
  previewAssistantId.value = id
  assistantSrc.value = roleBgUrl(id)
}

async function confirmAssistant() {
  await updateAssistant({ userId: userId.value, assistantId: previewAssistantId.value })
  const user = JSON.parse(localStorage.getItem('user'))
  user.assistantId = previewAssistantId.value
  localStorage.setItem('user', JSON.stringify(user))
  userAssistantId.value = previewAssistantId.value
  showAssistantPanel.value = false
  ElMessage.success('助理已更换')
}

function cancelAssistant() {
  assistantSrc.value = roleBgUrl(userAssistantId.value)
  showAssistantPanel.value = false
}

function openTheme() {
  if (!showThemePanel.value) {
    originalTheme.value = bgmAudio.value ? bgmAudio.value.src : ''
    selectedTheme.value = ''
  }
  showThemePanel.value = !showThemePanel.value
}

function previewTheme(theme) {
  selectedTheme.value = theme
  if (bgmAudio.value) {
    bgmAudio.value.pause()
  }
  bgmAudio.value = new Audio(homepageBgmUrl(theme))
  bgmAudio.value.loop = true
  applyMusicVolume()
  bgmAudio.value.play().catch(() => {})
}

async function confirmTheme() {
  if (!selectedTheme.value) {
    ElMessage.warning('请选择主题')
    return
  }
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  try {
    const res = await updateBackgroundMusic({
      userId: user.id,
      backgroundMusic: selectedTheme.value
    })
    if (res.success) {
      user.background_music = selectedTheme.value
      localStorage.setItem('user', JSON.stringify(user))
      showThemePanel.value = false
      ElMessage.success('主题已更换')
    } else {
      ElMessage.error(res.message || '更换失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('网络异常，更换失败')
  }
}

function cancelTheme() {
  if (bgmAudio.value) {
    bgmAudio.value.pause()
  }
  const bgmFile = (JSON.parse(localStorage.getItem('user') || '{}')).background_music || '1'
  bgmAudio.value = new Audio(homepageBgmUrl(bgmFile))
  bgmAudio.value.loop = true
  applyMusicVolume()
  bgmAudio.value.play().catch(() => {})
  showThemePanel.value = false
}

function openScene() {
  if (!showScenePanel.value) {
    originalBg.value = bgSrc.value
    selectedBg.value = ''
  } else {
    bgSrc.value = originalBg.value
    bgFailed.value = false
  }
  showScenePanel.value = !showScenePanel.value
}

function previewBg(bg) {
  selectedBg.value = bg
  bgSrc.value = bgUrl(bg)
  bgFailed.value = false
}

async function confirmBg() {
  if (!selectedBg.value) {
    ElMessage.warning('请选择背景')
    return
  }
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  try {
    const res = await updateBackground({
      userId: user.id,
      backgroundId: selectedBg.value.replace('.webp', '')
    })
    if (res.success) {
      user.backgroundId = selectedBg.value.replace('.webp', '')
      localStorage.setItem('user', JSON.stringify(user))
      showScenePanel.value = false
      ElMessage.success('背景已更换')
    } else {
      ElMessage.error(res.message || '更换失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('网络异常，更换失败')
  }
}

</script>

<style scoped>
.home-page {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
  position: relative;
  overflow: hidden;
}
.game-overlay {
  position: absolute; top: 0; left: 0;
  width: 100%; height: 100%; z-index: 150;
}
.transition-blackout {
  position: fixed; top: 0; left: 0;
  width: 100%; height: 100%;
  background: #000; z-index: 200;
  pointer-events: none;
  animation: blackout 1.2s ease forwards;
}
@keyframes blackout {
  0% { opacity: 0; }
  33% { opacity: 1; }
  66% { opacity: 1; }
  100% { opacity: 0; }
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.4s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.home-bg {
  position: absolute; top: 0; left: 0;
  width: 100%; height: 100%;
  object-fit: cover;
  z-index: 0;
}
.home-assistant {
  position: absolute; bottom: 0; left: 0;
  width: auto; height: 80%;
  z-index: 1; pointer-events: none;
}
/* 提高所有按钮层级到背景之上 */
.top-left, .mid-left, .bottom-left, .right-btns { z-index: 2; }

/* 上窗口面板 */
.top-slide-panel {
  position: absolute; top: 0; left: 0;
  width: 100%; height: 100px;
  z-index: 10;
  transform: translateY(-100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.85) 0%,
    rgba(0, 0, 0, 0.6) 85%,
    transparent 100%
  );
  padding: 20px 0 40px;
}
.top-show {
  transform: translateY(0);
}

/* 下窗口面板 */
.bottom-slide-panel {
  position: absolute; bottom: 0; left: 0;
  width: 100%;
  z-index: 10;
  transform: translateY(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.85) 0%,
    rgba(0, 0, 0, 0.6) 85%,
    transparent 100%
  );
  padding: 40px 0 20px;
  display: flex; align-items: center; justify-content: center;
  padding-bottom: 2%;
}
.bottom-show {
  transform: translateY(0);
}

/* 背景切换面板 */
.bg-panel {
  position: absolute; top: 0; left: 0;
  width: 100%; height: 100%;
  z-index: 10;
}

.bottom-actions {
  display: flex; align-items: center; gap: 0;
  padding: 20px 0;
}
.action-item {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 0 40px;
  color: #fff; cursor: pointer;
  font-size: 14px;
}
.action-item .el-icon {
  font-size: 28px;
}
.action-divider {
  width: 1px; height: 40px;
  background: rgba(255,255,255,0.4);
}

.btn-back {
  position: absolute; top: 24px; left: 20px;
  z-index: 11;
  background: #333 !important;
  border: none !important;
  color: #fff !important;
  font-size: 20px;
  padding: 6px 64px 6px 8px;
  border-radius: 0;
  justify-content: flex-start;
}

/* 场景选择面板 */
.scene-slide-panel {
  position: absolute; top: 0; right: 0;
  width: 320px; height: 100%;
  z-index: 12;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
  background: rgba(0, 0, 0, 0.85);
}
.scene-panel-top {
  display: flex; align-items: center; gap: 16px;
  padding: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.scene-panel-title {
  font-size: 22px; color: #fff;
}
.scene-show {
  transform: translateX(0);
}
.scene-list {
  flex: 1;
  overflow-y: auto;
  padding: 80px 16px 16px;
  display: flex; flex-direction: column; gap: 12px;
}
.scene-list::-webkit-scrollbar { width: 4px; }
.scene-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); border-radius: 2px; }
.scene-thumb {
  width: 100%; aspect-ratio: 16/9;
  border-radius: 6px; overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}
.scene-thumb img {
  width: 100%; height: 100%; object-fit: cover;
}
.scene-thumb:hover { border-color: rgba(255,255,255,0.5); }
.scene-selected { border-color: #ffd54f !important; }
.btn-confirm {
  position: absolute; bottom: 48px; right: 52px;
  z-index: 13;
  background: #333 !important;
  border: none !important;
  color: #fff !important;
  font-size: 18px;
  padding: 6px 48px;
  border-radius: 0;
}

/* 主题面板 */
.theme-panel {
  position: absolute; top: 0; right: 0;
  width: 320px; height: 100%;
  z-index: 12;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
  background: rgba(0, 0, 0, 0.85);
}
.theme-panel-top {
  display: flex; align-items: center; gap: 16px;
  padding: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.theme-panel-title {
  font-size: 22px; color: #fff;
}
.theme-panel-show {
  transform: translateX(0);
}
.theme-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 80px 16px 16px;
}
.theme-panel-body::-webkit-scrollbar { width: 4px; }
.theme-panel-body::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); border-radius: 2px; }
.theme-list {
  display: flex; flex-direction: column; gap: 12px;
}
.theme-item {
  width: 100%;
  padding: 20px 16px;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid transparent;
  background: rgba(255,255,255,0.05);
  transition: border-color 0.2s, background 0.2s;
  text-align: center;
}
.theme-item:hover {
  border-color: rgba(255,255,255,0.5);
  background: rgba(255,255,255,0.08);
}
.theme-selected {
  border-color: #ffd54f !important;
  background: rgba(255,213,79,0.1);
}
.theme-item-text {
  font-size: 18px; color: #fff;
}

/* 左上角 */
.top-left {
  position: absolute; top: 24px; left: 24px;
  display: flex; gap: 12px;
}

/* 左中间 */
.mid-left {
  position: absolute; top: 50%; left: 48px;
  transform: translateY(-50%);
  display: flex; align-items: center; gap: 8px;
}
.btn-refresh {
  width: 48px; height: 48px;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}
.btn-svg {
  width: 100%; height: 100%;
}

.username {
  font-size: 28px; color: #ffd54f; font-weight: bold;
  text-shadow: 0 0 20px rgba(255, 213, 79, 0.4);
}

/* 左下角 */
.bottom-left {
  position: absolute; bottom: 32px; left: 24px;
}

/* 右边按钮 */

.right-btns {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  width: 1000px;       /* 自由调整 */
  z-index: 2;
}

.svg-btn {
  position: relative;
  cursor: pointer;
  transition: opacity 0.4s ease, transform 0.2s;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.svg-btn:hover {
  transform: scale(1.05);
}

.btn-label {
  position: absolute;   /* 自由移动 */
  display: flex;
  align-items: center;
  gap: 4px;
  color: #000000;
  font-size: 18px;
  /* 默认位置，可被下面的具体按钮覆盖 */
  bottom: 10px;
  transform: translateX(-50%);
}

.btn-label img {
  width: 250px; height: auto;
}

.btn-start-svg {
  width: 980px;
  height: 324px;
  position: absolute;
  top: 130px;
  right: -70px;
}

.btn-start-svg .btn-label {
  top: 25px;
  left: 180px;
}

.btn-start-svg .btn-label img{
  width: 300px; height: auto;
}

.btn-quick-link {
  position: absolute;
  top: 60px;
  left: 500px;
  width: 200px;
  height: 100px;
}

.btn-create-room {
  position: absolute;
  top: 160px;
  left: 500px;
  width: 200px;
  height: 100px;
}

.btn-records {
  position: absolute;
  top: 430px;
  right: 550px;
  width: 450px;     /* 自己调 */
  height: 270px;     /* 自己调 */
}

.btn-records .btn-label {
  font-size: 18px;
  top: 25px;
  left: 300px;
  height: 200px;
  width: 450px;
}

.btn-records .btn-label img {
  width: 300px; height: auto;
}

.btn-roles {
  position: absolute;
  top: 434px;
  right: 50px;
  width: 450px;
  height: 270px;
}

.btn-roles .btn-label {
  font-size: 18px;
  top: 25px;
  left: 300px;
  height: 200px;
  width: 450px;
}

.btn-roles .btn-label img {
  width: 300px; height: auto;
}

.btn-friends {
  position: absolute;
  top: 675px;
  right: 450px;
  width: 450px;
  height: 270px;
}

.btn-friends .btn-label {
  font-size: 18px;
  top: 25px;
  left: 300px;
  height: 200px;
  width: 450px;
}

.btn-friends .btn-label img {
  width: 300px; height: auto;
}

.btn-favorites {
  position: absolute;
  top: 694px;
  right: -50px;
  width: 450px;
  height: 270px;
}

.btn-favorites .btn-label {
  font-size: 18px;
  top: 25px;
  left: 300px;
  height: 200px;
  width: 450px;
}

.btn-favorites .btn-label img {
  width: 300px; height: auto;
}

/* 左上角按钮统一颜色 */
.top-left .el-button.btn-icon {
  background: transparent !important; border: none !important;
  color: #fff !important; font-size: 24px;
  width: 44px; height: 44px;
}
.top-left .el-button.btn-icon img {
  width: 36px; height: 36px;
}

/* 设置面板 */
.settings-panel {
  position: absolute; top: 0; left: 0;
  width: 960px; height: 100%;
  z-index: 10;
  background: rgba(0, 0, 0, 0.9);
  display: flex; flex-direction: column;
  transform: translateX(-100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.settings-show {
  transform: translateX(0);
}
.settings-top {
  display: flex; align-items: center; gap: 16px;
  padding: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.settings-title {
  font-size: 22px; color: #fff;
}
.settings-body {
  flex: 1; display: flex;
}
.settings-tabs {
  width: 180px;
  border-right: 1px solid rgba(255,255,255,0.1);
  padding: 16px 0;
  display: flex; flex-direction: column; gap: 12px;
}
.settings-tab {
  padding: 12px 24px;
  color: #999; cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}
.settings-tab:hover { color: #fff; background: rgba(255,255,255,0.05); }
.settings-tab.active { color: #ffd54f; background: rgba(255,213,79,0.1); }
.settings-content {
  flex: 1; padding: 24px;
}
.tab-panel {
  color: #ccc; font-size: 15px;
}
.setting-item {
  margin: 0; padding: 8px 0;
}
.setting-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0;
  color: #ccc; font-size: 15px;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.setting-hint {
  margin: 0; padding: 0;
  color: #777; font-size: 12px;
}
.sound-right {
  display: flex; align-items: center; gap: 8px;
  flex-shrink: 0;
}

/* 用户中心 */
.user-actions {
  display: flex; flex-direction: column; gap: 12px;
  align-items: flex-start;
}
.btn-user {
  width: 200px !important;
  display: flex !important;
  background: #333 !important; border: none !important;
  color: #fff !important; font-size: 16px;
  padding: 10px 20px; border-radius: 0;
  justify-content: flex-start;
  margin: 0 !important;
}
.btn-user:hover { background: #444 !important; }
.btn-logout { color: #ff6b6b !important; }

/* 启动面板 */
.launch-panel {
  position: absolute; top: 0; right: 0;
  width: 960px; height: 100%;
  background: rgba(0, 0, 0, 1);
  z-index: 100;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
}
.launch-show {
  transform: translateX(0);
}
.launch-header {
  display: flex; align-items: center; gap: 16px;
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.launch-title {
  font-size: 22px; color: #fff;
}
.launch-body {
  flex: 1;
  display: flex; flex-direction: column;
}
.launch-row {
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  border-bottom: 1px solid rgba(255,255,255,0.15);
}
.launch-row:last-child {
  border-bottom: none;
}
.launch-btn {
  width: 100%; height: 100%;
  background: transparent !important;
  border: none !important;
  color: #fff !important;
  font-size: 24px;
  border-radius: 0;
  display: flex; align-items: center; gap: 12px;
  justify-content: center;
}

/* 创建房间面板 */
.room-panel {
  position: absolute; top: 0; right: 0;
  width: 640px; height: 100%;
  background: rgba(0, 0, 0, 1);
  z-index: 101;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
}
.room-show {
  transform: translateX(0);
}
/* 创建房间 */
.room-body {
  padding: 24px;
  display: flex; flex-direction: column;
  gap: 24px;
  overflow-y: auto;
}
.room-slots {
  display: flex; gap: 24px;
}
.room-slot.empty-slot {
  cursor: pointer;
}
.room-slot.empty-slot:hover {
  border-color: rgba(255,255,255,0.4);
  background: rgba(255,255,255,0.03);
}
.room-waiting {
  color: #888;
  text-align: center;
  padding: 40px;
  font-size: 16px;
}

.btn-start-sim {
  width: 100% !important;
  margin: 16px 0;
  background: #555 !important;
  border: none !important;
  color: #999 !important;
  font-size: 18px;
  padding: 12px;
  border-radius: 0;
  cursor: not-allowed;
}
.btn-start-sim-ready {
  background: #4caf50 !important;
  color: #fff !important;
  cursor: pointer;
}

.btn-leave {
  width: 100% !important;
  margin: 8px 0 16px;
  background: #c0392b !important;
  border: none !important;
  color: #fff !important;
  border-radius: 0;
  font-size: 14px;
  padding: 10px;
}

.btn-ready {
  background: #4caf50 !important;
  border: none !important;
  color: #fff !important;
  border-radius: 0;
  margin-top: 8px;
}
.btn-ready:disabled {
  background: #555 !important;
  color: #999 !important;
}
.btn-ready-done {
  background: #388e3c !important;
}
.btn-ready-placeholder {
  display: block;
  height: 32px;
  margin-top: 8px;
}

.slot-ready {
  border-color: #4caf50 !important;
  box-shadow: 0 0 12px rgba(76, 175, 80, 0.4);
}
.room-slot {
  flex: 1;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 12px;
  border: 1px dashed rgba(255,255,255,0.2);
  border-radius: 8px;
  padding: 24px;
}
.room-friends {
  flex: 1;
  overflow-y: auto;
}
.room-friends h4 {
  margin: 0 0 12px;
  font-size: 16px; color: #aaa;
}
.slot-label {
  font-size: 14px; color: #888;
}
.slot-avatar {
  width: 80px; height: 80px;
  object-fit: cover;
  background: #fff;
}
.slot-avatar.empty {
  display: flex; align-items: center; justify-content: center;
  font-size: 36px; color: #555;
  background: rgba(255,255,255,0.05);
}
.slot-name {
  font-size: 14px; color: #ccc;
}

/* 好友面板 */
.friends-panel {
  position: absolute; top: 0; right: 0;
  width: 960px; height: 100%;
  background: rgba(0, 0, 0, 0.9);
  z-index: 100;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
}
.friends-panel-show {
  transform: translateX(0);
}
.friends-panel-top {
  display: flex; align-items: center; gap: 16px;
  padding: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.friends-panel-title {
  font-size: 22px; color: #fff;
}
.friends-panel-body {
  flex: 1; display: flex;
}
.friends-panel-tabs {
  width: 140px;
  border-right: 1px solid rgba(255,255,255,0.1);
  padding: 12px 0;
}
.friends-panel-tab {
  padding: 14px 20px;
  font-size: 15px; color: #aaa;
  cursor: pointer;
  transition: all 0.2s;
}
.friends-panel-tab:hover { color: #fff; background: rgba(255,255,255,0.05); }
.friends-panel-tab.active { color: #ffd54f; background: rgba(255,213,79,0.1); }
.friends-panel-content {
  flex: 1; padding: 24px;
  overflow-y: auto;
}
.profile-card {
  text-align: center; padding: 40px;
}
.profile-avatar {
  width: 90px; height: 90px;
  border-radius: 0;
  object-fit: cover;
  margin-bottom: 16px;
  background: #fff;
}
.profile-card h3 { margin: 0; font-size: 20px; color: #fff; }
.profile-card p { color: #888; }
.section { margin-bottom: 24px; }
.section h4 { margin: 0 0 12px; font-size: 16px; color: #aaa; }
.friend-item {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255,255,255,0.05);
  border-radius: 8px; margin-bottom: 8px;
}
.item-actions { display: flex; gap: 8px; }
.search-box {
  display: flex; gap: 12px; margin-bottom: 16px;
}
.search-box .el-input { width: 240px; }
.search-avatar {
  width: 44px; height: 44px;
  border-radius: 0;
  object-fit: cover;
  flex-shrink: 0;
  background: #fff;
}
.search-info {
  flex: 1; display: flex; flex-direction: column;
  gap: 2px;
}
.search-name { font-size: 15px; color: #fff; }
.search-id { font-size: 12px; color: #888; }
.friend-time {
  font-size: 12px; color: #666;
  flex: 1; text-align: center;
}
.empty { color: #666; padding: 20px; text-align: center; }

/* 助理面板 */
.assistant-panel {
  position: absolute; top: 0; right: 0;
  width: 960px; height: 100%;
  background: rgba(0, 0, 0, 0.9);
  z-index: 100;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
}
.assistant-panel-show {
  transform: translateX(0);
}
.assistant-panel-top {
  display: flex; align-items: center; gap: 16px;
  padding: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.assistant-panel-title {
  font-size: 22px; color: #fff;
}
.assistant-panel-body {
  flex: 1; overflow-y: auto; padding: 24px;
}
.assistant-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 16px;
}
.assistant-item {
  display: flex; flex-direction: column; align-items: center;
  padding: 12px 8px;
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(255,255,255,0.03);
}
.assistant-item:hover {
  background: rgba(255,255,255,0.08);
  border-color: rgba(255,255,255,0.2);
}
.assistant-item.assistant-selected {
  border-color: #ffd54f;
  background: rgba(255,213,79,0.1);
}
.assistant-avatar {
  width: 72px; height: 72px;
  object-fit: cover;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 8px;
}
.assistant-id {
  font-size: 12px; color: #aaa;
}
.assistant-panel-bottom {
  display: flex; justify-content: center; gap: 12px;
  padding: 20px;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.btn-confirm {
  background: #ffd54f; color: #000; border: none;
}
.btn-confirm:hover { background: #ffc107; }

/* 角色面板 */
.role-panel {
  position: absolute; top: 0; right: 0;
  width: 960px; height: 100%;
  background: rgba(0, 0, 0, 0.9);
  z-index: 100;
  transform: translateX(100%);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
}
.role-panel-show { transform: translateX(0); }
.role-panel-top {
  display: flex; align-items: center; gap: 16px;
  padding: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.role-panel-title { font-size: 22px; color: #fff; }
.role-panel-body {
  flex: 1; display: flex; flex-direction: column;
  overflow: hidden;
}
.role-detail {
  display: flex; gap: 24px;
  padding: 24px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  min-height: 280px;
}
.role-detail-left {
  width: 200px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.role-portrait {
  width: 100%; max-height: 240px;
  object-fit: contain;
}
.role-portrait-empty {
  color: #666; font-size: 14px; text-align: center;
}
.role-detail-right {
  flex: 1; color: #ccc; font-size: 14px;
  overflow-y: auto;
}
.role-detail-right h3 {
  font-size: 20px; color: #fff; margin-bottom: 12px;
}
.role-stats p {
  margin: 4px 0; color: #aaa;
}
.role-skill-desc, .role-passive-desc {
  margin-top: 12px;
}
.role-skill-desc p, .role-passive-desc p {
  margin: 2px 0; color: #bbb;
}
.role-remark-text {
  margin-top: 12px; padding: 8px 12px;
  background: rgba(255,255,255,0.05);
  border-radius: 4px;
}
.role-remark-text p { color: #999; font-style: italic; }
.role-detail-empty {
  color: #666; font-size: 16px;
  display: flex; align-items: center; height: 100%;
}
.role-list {
  flex: 1; display: flex; flex-wrap: wrap;
  align-content: flex-start; gap: 12px;
  padding: 20px 24px;
  overflow-y: auto;
}
.role-list-item {
  display: flex; flex-direction: column; align-items: center;
  width: 90px; padding: 8px;
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(255,255,255,0.03);
}
.role-list-item:hover {
  background: rgba(255,255,255,0.08);
  border-color: rgba(255,255,255,0.2);
}
.role-list-item-selected {
  border-color: #ffd54f;
  background: rgba(255,213,79,0.1);
}
.role-list-icon {
  width: 64px; height: 64px;
  object-fit: contain;
  margin-bottom: 6px;
}
.role-list-name {
  font-size: 12px; color: #aaa;
  text-align: center;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 100%;
}

/* 邀请弹窗 */
.invite-overlay {
  position: fixed; top: 0; left: 0;
  width: 100%; height: 100%;
  background: rgba(0,0,0,0.6);
  z-index: 200;
  display: flex; align-items: center; justify-content: center;
}
.invite-popup {
  background: #1a1a2e;
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  color: #fff;
  min-width: 280px;
}
.invite-popup h3 { margin: 0 0 16px; font-size: 20px; }
.invite-popup p { margin: 12px 0; font-size: 16px; color: #ccc; }
.invite-avatar {
  width: 72px; height: 72px;
  object-fit: cover;
  background: #fff;
}
.invite-actions {
  display: flex; gap: 12px; justify-content: center;
  margin-top: 20px;
}

.match-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.match-box {
  background: #1a1a2e;
  border: 2px solid #4fc3f7;
  border-radius: 12px;
  padding: 30px 50px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #fff;
  font-size: 18px;
}
.match-loading {
  width: 40px;
  height: 40px;
  border: 3px solid #4fc3f7;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>