import { render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import ChatInput from '@/features/chat/components/ChatInput.vue'

describe('ChatInput', () => {
  it('emits trimmed content and clears input', async () => {
    const { emitted } = render(ChatInput)
    const input = screen.getByPlaceholderText('输入问题，Agent 会结合知识库、工具调用和执行状态回答')

    await userEvent.type(input, '  查询 API 状态  ')
    await userEvent.click(screen.getByRole('button', { name: '发送' }))

    expect(emitted('send')).toEqual([['查询 API 状态']])
    expect(input).toHaveValue('')
  })

  it('emits cancel while streaming', async () => {
    const { emitted } = render(ChatInput, {
      props: {
        streaming: true,
      },
    })

    await userEvent.click(screen.getByRole('button', { name: '停止' }))

    expect(emitted('cancel')).toEqual([[]])
  })
})
