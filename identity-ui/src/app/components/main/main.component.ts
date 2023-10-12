import { Component, OnInit } from '@angular/core';
import { PrincipleService } from '../../services/principle/principle.service';

@Component({
    selector: 'app-main',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.scss']
})
export class MainComponent implements OnInit {

    isAuthenticated: Boolean = undefined;
    isScaAuthor: Boolean = undefined;

    constructor(private principle: PrincipleService) {
    }

    ngOnInit() {
        if (this.principle.isIdentityResolved()) {
            this.isAuthenticated = this.principle.isAuthenticated();
            this.isScaAuthor = this.principle.isInRole('ROLE_ihtsdo-sca-author');
        }

        this.principle.loadAccountCompleted.subscribe(() => {
            this.isAuthenticated = this.principle.isAuthenticated();
            this.isScaAuthor = this.principle.isInRole('ROLE_ihtsdo-sca-author');
        });
    }
}
