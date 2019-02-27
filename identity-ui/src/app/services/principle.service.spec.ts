import { TestBed } from '@angular/core/testing';

import { PrincipleService } from './principle.service';

describe('PrincipleService', () => {
    beforeEach(() => TestBed.configureTestingModule({}));

    it('should be created', () => {
        const service: PrincipleService = TestBed.get(PrincipleService);
        expect(service).toBeTruthy();
    });
});
