import { TestBed } from '@angular/core/testing';
import {
   HttpTestingController,
   provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { CompanyService } from './company.service';
import { CompanyProfileResponse } from '../models/company.model';
import { environment } from '../../environments/environment';

describe('CompanyService', () => {
   let service: CompanyService;
   let httpMock: HttpTestingController;

   const mockProfile: CompanyProfileResponse = {
      symbol: 'AAPL',
      companyName: 'Apple Inc.',
      industry: 'Technology',
      image: 'https://example.com/aapl.png',
      beta: 1.2,
      marketCap: 3000000000000,
      description: 'Apple designs consumer electronics.',
      price: 195.5,
   };

   beforeEach(() => {
      TestBed.configureTestingModule({
         providers: [
            CompanyService,
            provideHttpClient(),
            provideHttpClientTesting(),
         ],
      });

      service = TestBed.inject(CompanyService);
      httpMock = TestBed.inject(HttpTestingController);
   });

   afterEach(() => {
      httpMock.verify();
   });

   it('should be created', () => {
      expect(service).toBeTruthy();
   });

   it('should retrieve company profile by ticker', () => {
      service.getCompanyProfile('AAPL').subscribe({
         next: (profile) => {
            expect(profile).toEqual(mockProfile);
         },
      });

      const req = httpMock.expectOne(
         `${environment.apiUrl}/company-profile/AAPL`,
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProfile);
   });

   it('should propagate HTTP error when request fails', () => {
      service.getCompanyProfile('UNKNOWN').subscribe({
         next: () => expect.unreachable('Should have failed'),
         error: (error) => {
            expect(error.status).toBe(404);
         },
      });

      const req = httpMock.expectOne(
         `${environment.apiUrl}/company-profile/UNKNOWN`,
      );
      req.flush({ detail: 'Company not found' }, { status: 404, statusText: 'Not Found' });
   });
});
