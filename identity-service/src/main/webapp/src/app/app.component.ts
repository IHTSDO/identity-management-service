import { Component } from '@angular/core';
import { environment } from '../environments/environment';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html'
})
export class AppComponent {

    env: string;

    constructor() {
        if (environment.production) {
            this.env = '';
        } else if (environment.uat) {
            this.env = 'uat';
        } else {
            this.env = 'dev';
        }
    }

}
