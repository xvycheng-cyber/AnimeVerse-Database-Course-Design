<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>

<template>
  <div class="auth-page">
    <div class="auth-page__visual">
      <RouterLink to="/" class="auth-brand">
        <span><el-icon><MagicStick /></el-icon></span>
        <div>
          <strong>星漫云</strong>
          <small>ANIMEVERSE</small>
        </div>
      </RouterLink>
      <div class="auth-art" aria-hidden="true">
        <div class="auth-art__orbit auth-art__orbit--one" />
        <div class="auth-art__orbit auth-art__orbit--two" />
        <div class="auth-art__core">
          <el-icon><Film /></el-icon>
        </div>
        <div class="auth-art__caption">
          <span>DATABASE COURSE DESIGN</span>
          <strong>内容、关系与互动的完整数据链路</strong>
        </div>
      </div>
    </div>

    <div class="auth-page__form-side">
      <div class="auth-form-wrap">
        <RouterLink to="/" class="auth-back">
          <el-icon><ArrowLeft /></el-icon>
          返回动漫库
        </RouterLink>
        <header>
          <span class="page-heading__eyebrow">{{ eyebrow }}</span>
          <h1>{{ title }}</h1>
          <p>{{ description }}</p>
        </header>
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(360px, 1.08fr) minmax(420px, 0.92fr);
  background: #090e1c;
  color: var(--user-text);
}

.auth-page__visual {
  position: relative;
  display: flex;
  overflow: hidden;
  min-height: 100vh;
  flex-direction: column;
  padding: 42px 54px;
  background:
    radial-gradient(circle at 74% 30%, rgba(123, 93, 244, 0.34), transparent 24rem),
    radial-gradient(circle at 18% 78%, rgba(42, 204, 194, 0.13), transparent 22rem),
    linear-gradient(145deg, #11172d, #0b1020 65%);
}

.auth-page__visual::after {
  position: absolute;
  inset: 0;
  opacity: 0.12;
  background-image:
    linear-gradient(rgba(161, 175, 220, 0.18) 1px, transparent 1px),
    linear-gradient(90deg, rgba(161, 175, 220, 0.18) 1px, transparent 1px);
  background-size: 46px 46px;
  content: "";
  mask-image: linear-gradient(to bottom, black, transparent 82%);
}

.auth-brand {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-brand > span {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 11px;
  background: linear-gradient(135deg, #7565ef, #3e7cfa);
  font-size: 22px;
}

.auth-brand > div {
  display: flex;
  flex-direction: column;
}

.auth-brand strong {
  font-size: 18px;
}

.auth-brand small {
  margin-top: 5px;
  color: #7f8bac;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.auth-art {
  position: relative;
  z-index: 2;
  display: grid;
  width: min(520px, 84%);
  aspect-ratio: 1;
  margin: auto;
  place-items: center;
}

.auth-art__orbit {
  position: absolute;
  border: 1px solid rgba(159, 146, 255, 0.28);
  border-radius: 50%;
}

.auth-art__orbit--one {
  width: 82%;
  height: 82%;
  transform: rotate(-18deg) scaleY(0.56);
}

.auth-art__orbit--two {
  width: 62%;
  height: 62%;
  border-color: rgba(77, 212, 198, 0.24);
  transform: rotate(34deg) scaleY(0.64);
}

.auth-art__core {
  display: grid;
  width: 150px;
  height: 150px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 34px;
  color: #ddd8ff;
  background: linear-gradient(145deg, rgba(110, 91, 235, 0.54), rgba(30, 42, 79, 0.56));
  box-shadow: 0 30px 90px rgba(78, 57, 203, 0.4);
  font-size: 62px;
  transform: rotate(-7deg);
  backdrop-filter: blur(18px);
}

.auth-art__caption {
  position: absolute;
  right: 4%;
  bottom: 2%;
  left: 4%;
  padding: 18px 20px;
  border-left: 2px solid var(--user-secondary);
  background: rgba(9, 14, 28, 0.38);
  backdrop-filter: blur(10px);
}

.auth-art__caption span {
  color: var(--user-secondary);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.auth-art__caption strong {
  display: block;
  margin-top: 7px;
  font-size: 14px;
}

.auth-page__form-side {
  display: grid;
  padding: 38px;
  place-items: center;
  background: #0b1120;
}

.auth-form-wrap {
  width: min(410px, 100%);
}

.auth-back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 54px;
  color: var(--user-muted);
  font-size: 13px;
}

.auth-back:hover {
  color: #fff;
}

.auth-form-wrap header {
  margin-bottom: 28px;
}

.auth-form-wrap h1 {
  margin: 0;
  font-size: 32px;
  letter-spacing: -0.04em;
}

.auth-form-wrap header p {
  margin: 10px 0 0;
  color: var(--user-muted);
  font-size: 13px;
  line-height: 1.7;
}

.auth-form-wrap :deep(.el-form-item) {
  margin-bottom: 20px;
}

.auth-form-wrap :deep(.el-form-item__label) {
  color: #c8cee2;
  font-size: 12px;
}

.auth-form-wrap :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 8px;
}

.auth-submit {
  width: 100%;
  min-height: 44px;
  margin-top: 4px;
}

.auth-switch {
  margin: 22px 0 0;
  color: var(--user-muted);
  font-size: 13px;
  text-align: center;
}

.auth-switch a {
  margin-left: 4px;
  color: var(--user-primary-strong);
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-page__visual {
    display: none;
  }

  .auth-page__form-side {
    min-height: 100vh;
    padding: 28px 20px;
  }

  .auth-back {
    margin-bottom: 38px;
  }
}
</style>
