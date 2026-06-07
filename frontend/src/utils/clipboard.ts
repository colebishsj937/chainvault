import { ElMessage } from 'element-plus'

export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
    return true
  } catch {
    ElMessage.error('复制失败')
    return false
  }
}
