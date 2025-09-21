import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../src/services/api', () => {
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    }
  };
});

import apiClient from '../src/services/api';
import tagService from '../src/services/tagService';

describe('tagService admin endpoints', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getTagsByBrand calls GET /auth/v1/group-tags/by-brand/:brandId', async () => {
    apiClient.get.mockResolvedValue({ data: [] });
    await tagService.getTagsByBrand(1, 0);
    expect(apiClient.get).toHaveBeenCalledWith('/auth/v1/group-tags/by-brand/1', { params: { parentId: 0 }, retryCount: 0 });
  });

  it('getTagTree calls GET /auth/v1/group-tags/tree/:brandId/full', async () => {
    apiClient.get.mockResolvedValue({ data: [] });
    await tagService.getTagTree(2);
    expect(apiClient.get).toHaveBeenCalledWith('/auth/v1/group-tags/tree/2/full', { retryCount: 0 });
  });

  it('createTag calls POST /auth/v1/group-tags', async () => {
    apiClient.post.mockResolvedValue({ data: { id: 10 } });
    const payload = { name: 'X', brandId: 1, parentId: 0 };
    const res = await tagService.createTag(payload);
    expect(apiClient.post).toHaveBeenCalledWith('/auth/v1/group-tags', payload);
    expect(res.id).toBe(10);
  });

  it('renameTag calls PUT /auth/v1/group-tags/:id?name=', async () => {
    apiClient.put.mockResolvedValue({ data: { id: 5, name: 'New' } });
    await tagService.renameTag(5, 'New');
    expect(apiClient.put).toHaveBeenCalledWith('/auth/v1/group-tags/5', null, { params: { name: 'New' }, retryCount: 0 });
  });

  it('moveTag calls PATCH /auth/v1/group-tags/:id/move?parentId=', async () => {
    apiClient.patch.mockResolvedValue({ data: { id: 5, parentId: 7 } });
    await tagService.moveTag(5, 7);
    expect(apiClient.patch).toHaveBeenCalledWith('/auth/v1/group-tags/5/move', null, { params: { parentId: 7 }, retryCount: 0 });
  });

  it('updateFull calls PUT /auth/v1/group-tags/:id/full with body', async () => {
    apiClient.put.mockResolvedValue({ data: { id: 1, name: 'N', parentId: 2, brandId: 3 } });
    const payload = { name: 'N', parentId: 2, brandId: 3 };
    await tagService.updateFull(1, payload);
    expect(apiClient.put).toHaveBeenCalledWith('/auth/v1/group-tags/1/full', payload, { retryCount: 0 });
  });

  it('deleteTag calls DELETE /auth/v1/group-tags/:id', async () => {
    apiClient.delete.mockResolvedValue({});
    await tagService.deleteTag(99);
    expect(apiClient.delete).toHaveBeenCalledWith('/auth/v1/group-tags/99', { retryCount: 0 });
  });
});
