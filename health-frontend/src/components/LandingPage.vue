<template>
  <div class="landing-container">
    <!-- 视频背景 -->
    <div class="video-background">
      <video
          autoplay
          loop
          muted
          playsinline
          class="bg-video"
          ref="videoRef"
      >
        <source src="/videos/223691.mp4" type="video/mp4" />
        您的浏览器不支持视频播放
      </video>
      <div class="overlay"></div>
    </div>

    <!-- 导航栏 - 按钮在右上角 -->
    <nav class="navbar">
      <div class="nav-actions">
        <button class="nav-btn" @click="$emit('open-login')">登录</button>
        <button class="nav-btn" @click="$emit('open-register')">注册</button>
      </div>
    </nav>

    <!-- 主要内容区 -->
    <main class="hero-section">
      <div class="hero-content">
        <div class="en-subtitle">THE WHOLE WORLD AWAITS</div>
        <h1 class="en-title">
          The whole<br />world awaits
        </h1>
        <h2 class="cn-title">
          自律，<span class="highlight">通往自由</span>
        </h2>
        <p class="subtitle">
          记录健康 · 坚持打卡 · 拥抱更好的自己
        </p>
        <div class="cta-buttons">
          <button class="primary-btn" @click="$emit('open-register')">开始旅程</button>
          <button class="secondary-btn" @click="$emit('open-login')">已有账号</button>
        </div>
      </div>
    </main>

    <!-- 向下滚动提示 -->
    <div class="scroll-hint">
      <span>探索更多</span>
      <div class="scroll-arrow"></div>
    </div>
  </div>
</template>

<script setup>
defineEmits(['open-login', 'open-register'])
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.landing-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow-x: hidden;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ========== 视频背景 ========== */
.video-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -2;
  overflow: hidden;
  background: #000;
}

.bg-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 50% 90%;
}

/* 蒙层 */
.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.15);
  z-index: 1;
}

/* ========== 导航栏 - 按钮在右上角 ========== */
.navbar {
  position: fixed;
  top: 0;
  right: 0;  /* 靠右对齐 */
  left: auto;  /* 取消左对齐 */
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 24px 48px;
  z-index: 100;
  background: transparent;
}

.nav-actions {
  display: flex;
  gap: 16px;
}

/* 导航栏按钮样式 - 统一透明边框 */
.nav-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: rgba(255, 255, 255, 0.8);
  padding: 8px 24px;
  border-radius: 40px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.8);
  color: white;
  transform: translateY(-1px);
}

/* ========== 主内容区 ========== */
.hero-section {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  z-index: 10;
}

.hero-content {
  text-align: center;
  color: white;
  padding: 0 24px;
  animation: fadeInUp 1s ease-out;
}

.en-subtitle {
  font-size: 12px;
  letter-spacing: 4px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 20px;
  text-transform: uppercase;
}

.en-title {
  font-size: 80px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -2px;
  margin-bottom: 30px;
  color: white;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
}

.cn-title {
  font-size: 32px;
  font-weight: 500;
  line-height: 1.3;
  margin-bottom: 20px;
  letter-spacing: -0.5px;
  color: rgba(255, 255, 255, 0.9);
}

.cn-title .highlight {
  font-weight: 300;
}

.subtitle {
  font-size: 16px;
  opacity: 0.6;
  margin-bottom: 48px;
  letter-spacing: 2px;
  font-weight: 300;
}

/* ========== 底部按钮 - 两个样式统一 ========== */
.cta-buttons {
  display: flex;
  gap: 24px;
  justify-content: center;
}

/* 两个按钮样式统一 */
.primary-btn,
.secondary-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 14px 40px;
  border-radius: 50px;
  font-size: 16px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s;
  letter-spacing: 1px;
}

/* 悬停效果 */
.primary-btn:hover,
.secondary-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.8);
  color: white;
  transform: translateY(-3px);
}

/* ========== 滚动提示 ========== */
.scroll-hint {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
  z-index: 20;
  animation: bounce 2s infinite;
  cursor: pointer;
}

.scroll-hint span {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.scroll-arrow {
  width: 24px;
  height: 24px;
  border-right: 2px solid rgba(255, 255, 255, 0.5);
  border-bottom: 2px solid rgba(255, 255, 255, 0.5);
  transform: rotate(45deg);
  margin: 0 auto;
}

/* ========== 动画 ========== */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bounce {
  0%, 100% {
    transform: translateX(-50%) translateY(0);
  }
  50% {
    transform: translateX(-50%) translateY(10px);
  }
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .en-title {
    font-size: 60px;
  }

  .cn-title {
    font-size: 28px;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 20px 24px;
  }

  .nav-btn {
    padding: 6px 16px;
    font-size: 12px;
  }

  .en-subtitle {
    font-size: 10px;
    letter-spacing: 3px;
  }

  .en-title {
    font-size: 40px;
  }

  .cn-title {
    font-size: 20px;
  }

  .subtitle {
    font-size: 13px;
  }

  .primary-btn,
  .secondary-btn {
    padding: 10px 24px;
    font-size: 14px;
  }

  .cta-buttons {
    gap: 16px;
  }
}
</style>