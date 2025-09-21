import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';

vi.mock('@/services/tagService', () => ({
  default: {
    getTagTree: vi.fn(),
    createTag: vi.fn(),
  }
}));

vi.mock('@/store/tag', () => {
  const __mocks = {
    createTag: vi.fn(),
    fetchTagsByBrand: vi.fn().mockResolvedValue([]),
  };
  return {
    useTagStore: () => __mocks,
    __mocks
  };
});

import tagService from '@/services/tagService';
import CreateGroupModal from '@/components/modals/CreateGroupModal.vue';

const brands = [ { id: 1, name: 'Brand1' } ];

function mountCreate(props = {}) {
  return mount(CreateGroupModal, {
    props: {
      brands,
      brandId: 1,
      parentId: 0,
      ...props
    },
    global: {
      stubs: {
        Modal: {
          template: '<div><slot name="content" /></div>'
        }
      }
    }
  });
}

describe('CreateGroupModal.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads parent list with a single tree request', async () => {
    tagService.getTagTree.mockResolvedValue([
      { id: 10, name: 'Root', children: [ { id: 11, name: 'Child', children: [] } ] }
    ]);

    const wrapper = mountCreate();
    await Promise.resolve();
    await wrapper.vm.$nextTick();

    expect(tagService.getTagTree).toHaveBeenCalled();
    const options = wrapper.findAll('option');
    // there should be at least: root option + 2 from tree
    expect(options.length).toBeGreaterThanOrEqual(3);
  });

  it('defaults parent to provided parentId (root -> "0")', async () => {
    tagService.getTagTree.mockResolvedValue([]); // empty -> fallback, but parent remains "0"
    const wrapper = mountCreate({ parentId: 0 });
    await Promise.resolve();
    await wrapper.vm.$nextTick();

    const select = wrapper.find('select#parentId');
    expect(select.element.value).toBe('0');
  });

  it('submits and calls tagService.createTag with correct payload (direct onSubmit)', async () => {
    tagService.getTagTree.mockResolvedValue([]);
    tagService.createTag.mockResolvedValue({ id: 100 });

    const wrapper = mountCreate();
    await wrapper.find('input#name').setValue('New Tag');
    // вызовем onSubmit напрямую, минуя асинхронную валидацию
    await wrapper.vm.onSubmit(
      { name: 'New Tag', brandId: 1, parentId: '0' },
      { resetForm: () => {} }
    );

    expect(tagService.createTag).toHaveBeenCalled();
    const payload = tagService.createTag.mock.calls[0][0];
    expect(payload).toMatchObject({ name: 'New Tag', brandId: 1, parentId: 0 });
  });
});
