import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { adminService } from '@/lib/api/adminService';
import { api, AppApiError } from '@/lib/api/client';
import { QueueStompManager } from '@/lib/websocket/stompClient';

vi.mock('@/lib/api/client', async () => {
  const actual = await vi.importActual<typeof import('@/lib/api/client')>('@/lib/api/client');
  return {
    ...actual,
    api: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    },
  };
});

describe('Integration Bug Fixes & Contract Verification', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('FIX 2 & 3: Admin User Status and Role Update Contracts', () => {
    it('sends PATCH with { status } payload and no reason field', async () => {
      vi.mocked(api.patch).mockResolvedValueOnce({
        success: true,
        message: 'Success',
        timestamp: new Date().toISOString(),
        data: {
          id: 1,
          fullName: 'Test User',
          email: 'user@example.com',
          phoneNumber: '1234567890',
          role: 'PATIENT',
          accountStatus: 'BLOCKED',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
      });

      const result = await adminService.updateUserStatus(1, 'BLOCKED');

      expect(api.patch).toHaveBeenCalledWith('/api/v1/admin/users/1/status', {
        status: 'BLOCKED',
      });
      expect(result.accountStatus).toBe('BLOCKED');
    });

    it('sends PATCH with { role } payload and no reason field', async () => {
      vi.mocked(api.patch).mockResolvedValueOnce({
        success: true,
        message: 'Success',
        timestamp: new Date().toISOString(),
        data: {
          id: 1,
          fullName: 'Test User',
          email: 'user@example.com',
          phoneNumber: '1234567890',
          role: 'DOCTOR',
          accountStatus: 'ACTIVE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
      });

      const result = await adminService.updateUserRole(1, 'DOCTOR');

      expect(api.patch).toHaveBeenCalledWith('/api/v1/admin/users/1/role', {
        role: 'DOCTOR',
      });
      expect(result.role).toBe('DOCTOR');
    });
  });

  describe('FIX 4: Department Status Toggle Contract', () => {
    it('sends PATCH with { active: boolean } in request body', async () => {
      vi.mocked(api.patch).mockResolvedValueOnce({
        success: true,
        message: 'Success',
        timestamp: new Date().toISOString(),
        data: {
          id: 10,
          name: 'Cardiology',
          active: true,
        },
      });

      const result = await adminService.updateDepartmentStatus(10, true);

      expect(api.patch).toHaveBeenCalledWith('/api/v1/admin/departments/10/status', {
        active: true,
      });
      expect(result.active).toBe(true);
    });

    it('sends PATCH with { active: false } when deactivating department', async () => {
      vi.mocked(api.patch).mockResolvedValueOnce({
        success: true,
        message: 'Success',
        timestamp: new Date().toISOString(),
        data: {
          id: 10,
          name: 'Cardiology',
          active: false,
        },
      });

      const result = await adminService.updateDepartmentStatus(10, false);

      expect(api.patch).toHaveBeenCalledWith('/api/v1/admin/departments/10/status', {
        active: false,
      });
      expect(result.active).toBe(false);
    });
  });

  describe('FIX 5: Queue Pause & Resume Return Contract', () => {
    it('calls pause endpoint and returns QueueSessionResponse', async () => {
      vi.mocked(api.post).mockResolvedValueOnce({
        success: true,
        message: 'Success',
        timestamp: new Date().toISOString(),
        data: {
          id: 42,
          doctorId: 7,
          isPaused: true,
          sessionStatus: 'ACTIVE',
        },
      });

      const result = await adminService.pauseQueue(42);

      expect(api.post).toHaveBeenCalledWith('/api/v1/admin/queues/42/pause');
      expect(result.id).toBe(42);
      expect(result.isPaused).toBe(true);
    });

    it('calls resume endpoint and returns QueueSessionResponse', async () => {
      vi.mocked(api.post).mockResolvedValueOnce({
        success: true,
        message: 'Success',
        timestamp: new Date().toISOString(),
        data: {
          id: 42,
          doctorId: 7,
          isPaused: false,
          sessionStatus: 'ACTIVE',
        },
      });

      const result = await adminService.resumeQueue(42);

      expect(api.post).toHaveBeenCalledWith('/api/v1/admin/queues/42/resume');
      expect(result.id).toBe(42);
      expect(result.isPaused).toBe(false);
    });
  });

  describe('FIX 7: API Error and Field Error Parsing', () => {
    it('instantiates AppApiError with field errors mapped from details string list', () => {
      const error = new AppApiError('Validation failed', 400, 'VALIDATION_ERROR', {
        email: 'Invalid email address',
        password: 'Password must be at least 8 characters',
      });

      expect(error.message).toBe('Validation failed');
      expect(error.status).toBe(400);
      expect(error.errorCode).toBe('VALIDATION_ERROR');
      expect(error.fieldErrors).toEqual({
        email: 'Invalid email address',
        password: 'Password must be at least 8 characters',
      });
    });
  });

  describe('FIX 6: WebSocket/STOMP Subscription Registry Lifecycle', () => {
    let stompManager: QueueStompManager;

    beforeEach(() => {
      stompManager = new QueueStompManager();
    });

    afterEach(() => {
      stompManager.disconnect();
    });

    it('manages subscription registry and permits clean unsubscribe before connection', () => {
      const callback = vi.fn();
      const sub = stompManager.subscribeToDoctorQueue(99, callback);

      expect(sub).toBeDefined();
      expect(typeof sub.unsubscribe).toBe('function');
      // Unsubscribe before connect removes entry cleanly
      sub.unsubscribe();
      stompManager.disconnect();
      expect(stompManager.getConnectionState()).toBe('DISCONNECTED');
    });

    it('registers multiple distinct subscriptions independently', () => {
      const cb1 = vi.fn();
      const cb2 = vi.fn();
      const sub1 = stompManager.subscribeToDoctorQueue(1, cb1);
      const sub2 = stompManager.subscribeToDoctorQueue(2, cb2);

      expect(typeof sub1.unsubscribe).toBe('function');
      expect(typeof sub2.unsubscribe).toBe('function');

      sub1.unsubscribe();
      sub2.unsubscribe();
    });
  });
});
