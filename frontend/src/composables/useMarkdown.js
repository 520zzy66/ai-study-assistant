import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const md = new MarkdownIt()

// DOMPurify 白名单配置：仅允许 Markdown 常用标签，排除 svg/math 等可触发脚本的标签
const PURIFY_CONFIG = {
  ALLOWED_TAGS: [
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'p', 'br', 'hr',
    'ul', 'ol', 'li',
    'blockquote', 'pre', 'code',
    'em', 'strong', 'del', 's',
    'a', 'img',
    'table', 'thead', 'tbody', 'tr', 'th', 'td',
    'span', 'div', 'input'
  ],
  ALLOWED_ATTR: [
    'href', 'src', 'alt', 'title', 'class', 'target', 'rel',
    'type', 'checked', 'disabled', 'colspan', 'rowspan'
  ],
  ALLOW_DATA_ATTR: false
}

/**
 * Markdown 渲染 composable
 * 统一使用 markdown-it + DOMPurify 进行渲染和 XSS 防护
 */
export function useMarkdown() {
  const renderMarkdown = (content) => DOMPurify.sanitize(md.render(content || ''), PURIFY_CONFIG)
  return { renderMarkdown }
}
