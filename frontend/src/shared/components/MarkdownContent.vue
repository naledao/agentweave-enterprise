<template>
  <div class="markdown-content" v-html="html"></div>
</template>

<script setup lang="ts">
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'
import { computed } from 'vue'

const props = defineProps<{
  content: string
}>()

const markdown = new MarkdownIt({
  breaks: true,
  html: false,
  linkify: true,
  typographer: true,
})

const html = computed(() => DOMPurify.sanitize(markdown.render(props.content)))
</script>

<style scoped>
.markdown-content {
  margin-top: 10px;
  color: #39485f;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.markdown-content :deep(*) {
  margin-top: 0;
}

.markdown-content :deep(*:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(p),
.markdown-content :deep(ul),
.markdown-content :deep(ol),
.markdown-content :deep(blockquote),
.markdown-content :deep(pre),
.markdown-content :deep(table) {
  margin-bottom: 10px;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 22px;
}

.markdown-content :deep(li + li) {
  margin-top: 4px;
}

.markdown-content :deep(strong) {
  color: #182233;
  font-weight: 700;
}

.markdown-content :deep(a) {
  color: #2f73ff;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(code) {
  border-radius: 4px;
  background: #eef3fb;
  color: #1f365c;
  padding: 2px 5px;
  font-family: "JetBrains Mono", Consolas, "Liberation Mono", monospace;
  font-size: 0.92em;
}

.markdown-content :deep(pre) {
  overflow-x: auto;
  border-radius: 6px;
  background: #101828;
  padding: 12px;
}

.markdown-content :deep(pre code) {
  background: transparent;
  color: #eef4ff;
  padding: 0;
}

.markdown-content :deep(blockquote) {
  border-left: 3px solid #9db8ee;
  color: #5a6880;
  padding-left: 12px;
}

.markdown-content :deep(table) {
  display: block;
  width: max-content;
  min-width: 60%;
  max-width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #d8dee8;
  padding: 6px 8px;
}

.markdown-content :deep(th) {
  background: #f5f7fb;
  color: #182233;
}
</style>
