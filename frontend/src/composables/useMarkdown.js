import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const md = new MarkdownIt()

/**
 * Markdown 渲染 composable
 * 统一使用 markdown-it + DOMPurify 进行渲染和 XSS 防护
 */
export function useMarkdown() {
  const renderMarkdown = (content) => DOMPurify.sanitize(md.render(content || ''))
  return { renderMarkdown }
}
