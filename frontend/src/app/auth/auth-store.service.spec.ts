import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthStoreService } from './auth-store.service';
import { AuthService } from './auth.service';
import { UserProfile } from '../models/user.model';
import { firstValueFrom } from 'rxjs';

describe('AuthStoreService', () => {
   let authStoreService: AuthStoreService;
   let authServiceMock: {
      getUserProfile: ReturnType<typeof vi.fn>;
      login: ReturnType<typeof vi.fn>;
      logout: ReturnType<typeof vi.fn>;
   };

   const mockUser: UserProfile = {
      roles: ['ADMIN'],
      username: 'Alice',
      email: '',
   };

   beforeEach(() => {
      authServiceMock = {
         getUserProfile: vi.fn().mockReturnValue(mockUser),
         login: vi.fn(),
         logout: vi.fn(),
      };
      authStoreService = new AuthStoreService(authServiceMock as unknown as AuthService);
      // Reset call count after initialization since refreshState is called in constructor
      authServiceMock.getUserProfile.mockClear();
   });

   it('should initialize with user profile', async () => {
      const user = await firstValueFrom(authStoreService.user$);
      expect(user).toEqual(mockUser);
      // Verify that getUserProfile is not called when subscribing to user$
      expect(authServiceMock.getUserProfile).not.toHaveBeenCalled();
   });
});
