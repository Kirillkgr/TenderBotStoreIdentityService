import {beforeEach, describe, expect, it, vi} from 'vitest';
import {mount} from '@vue/test-utils';
import tagService from '@/services/tagService';
import EditGroupModal from '@/components/modals/EditGroupModal.vue';

vi.mock('@/services/tagService', () => ({
  default: {
    getTagTree: vi.fn(),
    updateFull: vi.fn(),
  }
}));

vi.mock('@/store/tag', () => ({
  useTagStore: () => ({
    fetchTagsByBrand: vi.fn().mockResolvedValue([]),
  })
}));

const brands = [ { id: 1, name: 'Brand1' } ];
const tag = { id: 11, name: 'Child', brandId: 1, parentId: 10 };

function mountEdit(props = {}) {
  return mount(EditGroupModal, {
    props: {
      brands,
      tag,
      brandId: 1,
      ...props
    },
    global: {
      stubs: {
        Modal: { template: '<div><slot name="content" /></div>' }
      }
    }
  });
}

describe('EditGroupModal.vue', () => {
  beforeEach(() => vi.clearAllMocks());

  it('loads parents from single tree request and marks current parent', async () => {
    tagService.getTagTree.mockResolvedValue([
      { id: 10, name: 'Root', children: [ { id: 11, name: 'Child', children: [] } ] }
    ]);
    const wrapper = mountEdit();
    await Promise.resolve();
    await wrapper.vm.$nextTick();

    expect(tagService.getTagTree).toHaveBeenCalled();
    const select = wrapper.find('select#parentId');
    expect(select.element.value).toBe(String(tag.parentId));
  });

  it.skip('submits and calls tagService.updateFull with body', async () => {
    tagService.getTagTree.mockResolvedValue([]);
    tagService.updateFull.mockResolvedValue({ id: tag.id, name: 'New' });

    const wrapper = mountEdit();
    // change name
    await wrapper.find('input#name').setValue('New');

    await wrapper.find('form').trigger('submit.prevent');
    await Promise.resolve();
    await wrapper.vm.$nextTick();

    expect(tagService.updateFull).toHaveBeenCalled();
    const [id, payload] = tagService.updateFull.mock.calls[0];
    expect(id).toBe(tag.id);
    expect(payload).toMatchObject({ name: 'New', brandId: 1 });
  });
});
