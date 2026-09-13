/** SHA-256 hex digest, or undefined when `crypto.subtle` is unavailable. */
export async function sha256Hex(bytes: Uint8Array): Promise<string | undefined> {
  if (!globalThis.crypto?.subtle) {
    return undefined
  }
  const buffer = await crypto.subtle.digest('SHA-256', bytes as unknown as BufferSource)
  return Array.from(new Uint8Array(buffer))
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}
