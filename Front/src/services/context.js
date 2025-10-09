import api from './api';

// Context service: sets/clears httpOnly cookie `ctx` via backend endpoints
// POST /auth/v1/context { masterId, brandId, pickupPointId }
// DELETE /auth/v1/context

export async function setContext({masterId = null, brandId = null, pickupPointId = null} = {}) {
    const payload = {};
    if (masterId != null) payload.masterId = masterId;
    if (brandId != null) payload.brandId = brandId;
    if (pickupPointId != null) payload.pickupPointId = pickupPointId;
    const res = await api.post('/auth/v1/context', payload, {withCredentials: true});
    return res?.data ?? null;
}

export async function clearContext() {
    await api.delete('/auth/v1/context', {withCredentials: true});
}

export default {setContext, clearContext};
