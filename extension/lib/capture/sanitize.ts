import DOMPurify from 'dompurify'

/**
 * Extension-side sanitization (defense in depth only).
 *
 * The backend re-sanitizes independently with its own pinned policy; this pass
 * simply prevents obviously unsafe markup from ever leaving the browser.
 */
const ALLOWED_TAGS = [
  'a', 'abbr', 'article', 'aside', 'b', 'blockquote', 'br', 'caption', 'cite',
  'code', 'col', 'colgroup', 'dd', 'del', 'details', 'div', 'dl', 'dt', 'em',
  'figcaption', 'figure', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hr', 'i', 'img',
  'ins', 'kbd', 'li', 'mark', 'ol', 'p', 'pre', 'q', 's', 'samp', 'section',
  'small', 'span', 'strong', 'sub', 'summary', 'sup', 'table', 'tbody', 'td',
  'tfoot', 'th', 'thead', 'time', 'tr', 'u', 'ul', 'var', 'wbr',
]

const ALLOWED_ATTR = [
  'alt', 'cite', 'class', 'colspan', 'datetime', 'dir', 'headers', 'height',
  'href', 'lang', 'rel', 'rowspan', 'scope', 'span', 'src', 'start', 'title',
  'type', 'width',
]

export function sanitizeArticleHtml(html: string): string {
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false,
    ALLOWED_URI_REGEXP: /^(?:https?:|mailto:)/i,
  })
}
