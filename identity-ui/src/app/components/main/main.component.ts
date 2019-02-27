import { Component, OnInit } from '@angular/core';
import { PrincipleService } from '../../services/principle.service';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'app-main',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.scss']
})
export class MainComponent implements OnInit {

    isAuthenticated: Boolean = undefined;
    isScaAuthor: Boolean = undefined;
    sca_service: string;

    constructor(private principle: PrincipleService) {
        this.sca_service = environment.scaService;
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
