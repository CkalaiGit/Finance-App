import { TestBed } from '@angular/core/testing';

import { KeycloakService } from './keycloak.service';
import { beforeEach, describe, expect, it } from 'vitest';

describe('KeycloakService', () => {
  let service: KeycloakService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(KeycloakService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
