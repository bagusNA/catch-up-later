import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import LibraryEmptyState from '~/components/LibraryEmptyState.vue'
import LibraryErrorState from '~/components/LibraryErrorState.vue'

const stubs = {
  UIcon: true,
  UButton: true,
  UAlert: {
    props: ['title', 'description'],
    template: '<div class="alert">{{ title }} {{ description }}<slot name="actions" /></div>'
  }
}

describe('LibraryEmptyState', () => {
  it('renders the title and description', () => {
    const wrapper = mount(LibraryEmptyState, {
      props: { title: 'No matching items', description: 'Try another search.' },
      global: { stubs }
    })
    expect(wrapper.text()).toContain('No matching items')
    expect(wrapper.text()).toContain('Try another search.')
  })

  it('shows an action button only when a label is provided', async () => {
    const wrapper = mount(LibraryEmptyState, {
      props: { title: 'Empty', actionLabel: 'Clear filters' },
      global: { stubs }
    })
    const button = wrapper.find('u-button-stub')
    expect(button.exists()).toBe(true)
    await button.trigger('click')
    expect(wrapper.emitted('action')).toHaveLength(1)
  })
})

describe('LibraryErrorState', () => {
  it('renders the error message and emits retry', async () => {
    const wrapper = mount(LibraryErrorState, {
      props: { message: 'Could not load your library.' },
      global: { stubs }
    })
    expect(wrapper.text()).toContain('Could not load your library.')
    await wrapper.find('u-button-stub').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })
})
