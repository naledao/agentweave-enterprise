import { render, screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import type { UserProfile } from '@/features/auth/types'
import UserPasswordDrawer from '@/features/settings/components/UserPasswordDrawer.vue'

const user: UserProfile = {
  id: 'user-1',
  username: 'ops',
  displayName: '运维用户',
  email: 'ops@example.com',
  status: 'ACTIVE',
  roles: ['OPERATOR'],
  permissions: [],
  lastLoginAt: null,
}

describe('UserPasswordDrawer', () => {
  it('validates password confirmation', async () => {
    render(UserPasswordDrawer, {
      props: {
        modelValue: true,
        user,
      },
    })

    await userEvent.type(await screen.findByPlaceholderText('请输入新密码'), 'password123')
    await userEvent.type(await screen.findByPlaceholderText('请再次输入新密码'), 'password456')
    await userEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText('两次输入的密码不一致')).toBeInTheDocument()
  })

  it('emits submit with the new password', async () => {
    const { emitted } = render(UserPasswordDrawer, {
      props: {
        modelValue: true,
        user,
      },
    })

    await userEvent.type(await screen.findByPlaceholderText('请输入新密码'), 'password123')
    await userEvent.type(await screen.findByPlaceholderText('请再次输入新密码'), 'password123')
    await userEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(emitted('submit')).toEqual([['password123']])
    })
  })
})
