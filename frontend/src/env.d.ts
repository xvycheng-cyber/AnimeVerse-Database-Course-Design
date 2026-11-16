/// <reference types="vite/client" />

import type { Component } from 'vue'

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module 'vue' {
  export interface GlobalComponents {
    [name: string]: Component
  }
}

export {}
