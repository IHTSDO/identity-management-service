import { TestBed } from '@angular/core/testing';

import { PrincipleService } from './principle.service';
import { HttpClientModule } from '@angular/common/http';

describe('PrincipleService', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                HttpClientModule
            ]
        }).compileComponents();
    });

    it('should be created', () => {
        const service: PrincipleService = TestBed.get(PrincipleService);
        expect(service).toBeTruthy();
    });
});
