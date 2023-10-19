import { Component } from '@angular/core';

@Component({
    selector: 'app-snomed-footer',
    templateUrl: './snomed-footer.component.html',
    styleUrls: ['./snomed-footer.component.scss']
})
export class SnomedFooterComponent {

    year: number = new Date().getFullYear();


    constructor() {
    }
}
