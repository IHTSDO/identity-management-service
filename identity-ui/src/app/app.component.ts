import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {

    copyrightYear: Number = null;

    constructor(private http: HttpClient) {
    }

    ngOnInit() {
        this.http.get<any>('/', {observe: 'response'}).subscribe(() => {},
            response => {
                this.copyrightYear = new Date(response.headers.get('Date')).getFullYear();
            }
        );
    }
}
