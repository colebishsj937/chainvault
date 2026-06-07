<template>
  <router-view v-slot="{ Component, route }">
    <component :is="Component" v-if="Component" :key="route.fullPath" />
  </router-view>
</template>
