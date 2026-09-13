/**
 * Detects image formats from magic bytes. The backend independently validates
 * signatures, but the extension must never ship an asset whose bytes do not
 * match its declared MIME type (that would fail the whole package).
 */
export function sniffImageMime(bytes: Uint8Array): string | null {
  if (startsWith(bytes, [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])) return 'image/png'
  if (startsWith(bytes, [0xff, 0xd8, 0xff])) return 'image/jpeg'
  if (startsWith(bytes, [0x47, 0x49, 0x46, 0x38])) return 'image/gif'
  if (
    bytes.length >= 12 &&
    ascii(bytes, 0, 4) === 'RIFF' &&
    ascii(bytes, 8, 4) === 'WEBP'
  ) {
    return 'image/webp'
  }
  return null
}

function startsWith(bytes: Uint8Array, magic: number[]): boolean {
  if (bytes.length < magic.length) return false
  return magic.every((value, index) => bytes[index] === value)
}

function ascii(bytes: Uint8Array, offset: number, length: number): string {
  return Array.from(bytes.slice(offset, offset + length))
    .map(byte => String.fromCharCode(byte))
    .join('')
}
