import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import UserTable from '@/features/settings/components/UserTable.vue'
import type { UserProfile } from '@/features/auth/types'

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

describe('UserTable', () => {
  it('renders status tag', async () => {
    render(UserTable, {
      props: {
        users: [user],
        canWrite: true,
      },
    })

    expect(await screen.findByText('启用')).toBeInTheDocument()
  })

  it('hides write actions without permission', () => {
    render(UserTable, {
      props: {
        users: [user],
        canWrite: false,
      },
    })

    expect(screen.queryByText('编辑')).not.toBeInTheDocument()
    expect(screen.queryByText('角色')).not.toBeInTheDocument()
    expect(screen.queryByText('密码')).not.toBeInTheDocument()
    expect(screen.queryByText('禁用')).not.toBeInTheDocument()
  })

  it('shows reset password action with write permission', async () => {
    render(UserTable, {
      props: {
        users: [user],
        canWrite: true,
      },
    })

    expect(await screen.findByText('密码')).toBeInTheDocument()
  })
})
