import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const md = new MarkdownIt()

// 为外链自动添加 target="_blank" rel="noopener noreferrer"，防止钓鱼风险
const defaultRender = md.renderer.rules.link_open || function (tokens, idx, options, _env, self) {
  return self.renderToken(tokens, idx, options)
}
md.renderer.rules.link_open = function (tokens, idx, options, env, self) {
  const token = tokens[idx]
  const href = token.attrGet('href') || ''
  if (href.startsWith('http://') || href.startsWith('https://')) {
    token.attrSet('target', '_blank')
    token.attrSet('rel', 'noopener noreferrer')
  }
  return defaultRender(tokens, idx, options, env, self)
}

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
 *
 * ⚠️ 安全提醒：此函数的返回值已通过 DOMPurify 过滤，
 * 可安全用于 v-html。如需在其他地方渲染 Markdown，
 * 必须始终调用此函数处理，禁止直接使用 v-html 渲染原始内容。
 */
export function useMarkdown() {
  const renderMarkdown = (content) => DOMPurify.sanitize(md.render(content || ''), PURIFY_CONFIG)
  return { renderMarkdown }
}
