import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'
import dayjs from 'dayjs'

export interface ExportColumn {
  label: string
  key: string
  format?: (val: any, row: any) => string
}

/**
 * 将数据导出为 Excel 文件
 */
export function exportToExcel(
  data: Record<string, any>[],
  columns: ExportColumn[],
  filename: string,
) {
  const header = columns.map((c) => c.label)
  const rows = data.map((row) =>
    columns.map((c) => (c.format ? c.format(row[c.key], row) : (row[c.key] ?? ''))),
  )

  const ws = XLSX.utils.aoa_to_sheet([header, ...rows])
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')

  const buf = XLSX.write(wb, { type: 'array', bookType: 'xlsx' })
  saveAs(new Blob([buf], { type: 'application/octet-stream' }), `${filename}_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
}
