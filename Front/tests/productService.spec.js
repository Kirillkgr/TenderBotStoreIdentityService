import {beforeEach, describe, expect, it, vi} from 'vitest';
import apiClient from '../src/services/api';
import * as productService from '../src/services/productService';

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

describe('productService admin endpoints', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('createProduct calls POST /auth/v1/products', async () => {
    apiClient.post.mockResolvedValue({ data: { id: 1 } });
    const payload = { name: 'Test', price: 10, brandId: 1 };
    const res = await productService.createProduct(payload);
    expect(apiClient.post).toHaveBeenCalledWith('/auth/v1/products', payload);
    expect(res.data.id).toBe(1);
  });

  it('getProductById calls GET /auth/v1/products/:id', async () => {
    apiClient.get.mockResolvedValue({ data: { id: 5 } });
    await productService.getProductById(5);
    expect(apiClient.get).toHaveBeenCalledWith('/auth/v1/products/5');
  });

  it('updateProduct calls PUT /auth/v1/products/:id', async () => {
    const body = { name: 'New' };
    await productService.updateProduct(7, body);
    expect(apiClient.put).toHaveBeenCalledWith('/auth/v1/products/7', body);
  });

  it('changeProductBrand calls PATCH /auth/v1/products/:id/brand?brandId=', async () => {
    await productService.changeProductBrand(9, 2);
    expect(apiClient.patch).toHaveBeenCalledWith('/auth/v1/products/9/brand', null, { params: { brandId: 2 } });
  });

  it('getArchivedProductsByBrand calls GET /auth/v1/products/archive?brandId=', async () => {
    await productService.getArchivedProductsByBrand(3);
    expect(apiClient.get).toHaveBeenCalledWith('/auth/v1/products/archive', { params: { brandId: 3 } });
  });

  it('restoreFromArchive calls POST /auth/v1/products/archive/:id/restore', async () => {
    await productService.restoreFromArchive(11, 22);
    expect(apiClient.post).toHaveBeenCalledWith('/auth/v1/products/archive/11/restore', null, { params: { targetGroupTagId: 22 } });
  });

  it('purgeArchive calls DELETE /auth/v1/products/archive/purge', async () => {
    await productService.purgeArchive(90);
    expect(apiClient.delete).toHaveBeenCalledWith('/auth/v1/products/archive/purge', { params: { olderThanDays: 90 } });
  });

  it('deleteArchivedProduct calls DELETE /auth/v1/products/archive/:id', async () => {
    await productService.deleteArchivedProduct(15);
    expect(apiClient.delete).toHaveBeenCalledWith('/auth/v1/products/archive/15');
  });
});
