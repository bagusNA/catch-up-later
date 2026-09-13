import { describe, expect, it } from 'vitest'
import { sanitizeArticleHtml } from './sanitize'
import { RESERVED_ASSET_PREFIX } from './constants'

describe('sanitizeArticleHtml', () => {
  it('removes scripts, event handlers, and dangerous URLs', () => {
    const html = sanitizeArticleHtml(
      '<p onclick="steal()">Hello</p><script>alert(1)</script><a href="javascript:evil()">x</a>',
    )
    expect(html).not.toContain('<script')
    expect(html).not.toContain('onclick')
    expect(html).not.toContain('javascript:')
    expect(html).toContain('<p>Hello</p>')
  })

  it('keeps headings, lists, tables, and reserved-host images', () => {
    const html = sanitizeArticleHtml(
      `<h2>Heading</h2><ul><li>One</li></ul><table><tr><td>Cell</td></tr></table>` +
      `<img src="${RESERVED_ASSET_PREFIX}asset-1" alt="Hero">`,
    )
    expect(html).toContain('<h2>Heading</h2>')
    expect(html).toContain('<li>One</li>')
    expect(html).toContain('<td>Cell</td>')
    expect(html).toContain(`${RESERVED_ASSET_PREFIX}asset-1`)
  })
})
