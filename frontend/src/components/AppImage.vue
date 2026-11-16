<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { fallbackCover } from '@/utils/media'

const props = withDefaults(
  defineProps<{
    src?: string
    alt?: string
    ratio?: string
    rounded?: string
  }>(),
  {
    src: '',
    alt: '',
    ratio: '3 / 4',
    rounded: '8px'
  }
)

const failed = ref(false)
const currentSrc = computed(() => (failed.value ? fallbackCover : props.src || fallbackCover))

watch(
  () => props.src,
  () => {
    failed.value = false
  }
)
</script>

<template>
  <div
    class="app-image"
    :style="{ aspectRatio: ratio, borderRadius: rounded }"
  >
    <img
      :src="currentSrc"
      :alt="alt"
      loading="lazy"
      decoding="async"
      @error="failed = true"
    />
  </div>
</template>

<style scoped>
.app-image {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #151d33;
}

.app-image::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: "";
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
  border-radius: inherit;
}

.app-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
