import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';

import { SnomedFooterComponent } from './snomed-footer.component';

describe('SnomedFooterComponent', () => {
    let component: SnomedFooterComponent;
    let fixture: ComponentFixture<SnomedFooterComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                SnomedFooterComponent
            ],
            imports: [
                FormsModule
            ],
            schemas: []
        }).compileComponents();

        fixture = TestBed.createComponent(SnomedFooterComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
