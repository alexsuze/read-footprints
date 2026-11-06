// 書封上傳前壓縮成 JPEG data URL，限制在 maxKB 內（規劃書 4.3）
const MAX_SIDE = 900

export async function compressImage (file, maxKB = 500) {
  const bitmap = await createImageBitmap(file)
  const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(bitmap.width * scale)
  canvas.height = Math.round(bitmap.height * scale)
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#fff' // 透明 PNG 轉 JPEG 才不會變黑底
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height)

  const maxLength = maxKB * 1024 * 4 / 3 // Base64 比原檔大約 4/3
  let quality = 0.9
  let url = canvas.toDataURL('image/jpeg', quality)
  while (url.length > maxLength && quality > 0.2) {
    quality -= 0.1
    url = canvas.toDataURL('image/jpeg', quality)
  }
  return url
}
