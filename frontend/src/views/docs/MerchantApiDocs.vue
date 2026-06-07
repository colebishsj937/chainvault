<template>
  <div class="page-container merchant-api-docs">
    <PageHeader title="商户接口文档" />

    <el-alert
      class="mb-4"
      type="info"
      :closable="false"
      show-icon
      title="本文档面向商户技术对接方，可在对接时直接提供给商户开发人员。"
    />

    <el-card v-loading="loading">
      <div class="docs-toolbar">
        <div class="docs-meta">
          <el-tag type="primary" effect="plain">Gateway</el-tag>
          <span class="mono">{{ docs.gatewayBaseUrl || '—' }}</span>
          <span v-if="docs.version" class="text-secondary">v{{ docs.version }}</span>
          <span v-if="docs.updatedAt" class="text-secondary">更新于 {{ docs.updatedAt }}</span>
        </div>
        <div class="docs-actions">
          <el-button :icon="Download" @click="handleDownload">下载 Markdown</el-button>
          <el-button :icon="CopyDocument" @click="handleCopyUrl">复制 Gateway 地址</el-button>
        </div>
      </div>

      <article class="markdown-body" v-html="htmlContent" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MarkdownIt from 'markdown-it'
import { ElMessage } from 'element-plus'
import { Download, CopyDocument } from '@element-plus/icons-vue'
import { saveAs } from 'file-saver'
import PageHeader from '@/components/PageHeader.vue'
import { getMerchantDocs, type MerchantDocs } from '@/api/modules/docs'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
})

const loading = ref(false)
const docs = ref<MerchantDocs>({
  title: 'ChainVault 商户 API 对接文档',
  version: '',
  updatedAt: '',
  gatewayBaseUrl: '',
  markdown: '',
})

const htmlContent = computed(() => {
  if (!docs.value.markdown) {
    return ''
  }
  return md.render(docs.value.markdown)
})

async function loadDocs() {
  loading.value = true
  try {
    docs.value = await getMerchantDocs()
  } finally {
    loading.value = false
  }
}

function handleDownload() {
  if (!docs.value.markdown) {
    return
  }
  const blob = new Blob([docs.value.markdown], { type: 'text/markdown;charset=utf-8' })
  saveAs(blob, 'ChainVault-Merchant-API.md')
  ElMessage.success('文档已下载')
}

async function handleCopyUrl() {
  const url = docs.value.gatewayBaseUrl
  if (!url) {
    ElMessage.warning('Gateway 地址为空')
    return
  }
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('已复制 Gateway 地址')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(loadDocs)
</script>

<style scoped>
.docs-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.docs-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.docs-actions {
  display: flex;
  gap: 8px;
}
.text-secondary {
  color: #909399;
  font-size: 13px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}
.markdown-body {
  padding: 8px 4px 24px;
  line-height: 1.7;
  color: #303133;
  font-size: 14px;
}
.markdown-body :deep(h1) {
  font-size: 24px;
  margin: 0 0 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.markdown-body :deep(h2) {
  font-size: 20px;
  margin: 28px 0 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f2f5;
}
.markdown-body :deep(h3) {
  font-size: 16px;
  margin: 20px 0 8px;
}
.markdown-body :deep(h4) {
  font-size: 15px;
  margin: 16px 0 8px;
}
.markdown-body :deep(p),
.markdown-body :deep(li) {
  margin: 8px 0;
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
}
.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 13px;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #ebeef5;
  padding: 8px 12px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: #f5f7fa;
}
.markdown-body :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}
.markdown-body :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}
.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
.markdown-body :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 16px;
  border-left: 4px solid #409eff;
  background: #ecf5ff;
  color: #606266;
}
.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #ebeef5;
  margin: 24px 0;
}
</style>
