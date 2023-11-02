import { Component, OnInit } from '@angular/core';
import { Location} from '@angular/common';
import {Router} from '@angular/router';
import {User} from "../../models/user";
import {AuthenticationService} from "../../services/authentication/authentication.service";
import {PrincipleService} from "../../services/principle/principle.service";

@Component({
    selector: 'app-snomed-navbar',
    templateUrl: './snomed-navbar.component.html',
    styleUrls: ['./snomed-navbar.component.scss']
})
export class SnomedNavbarComponent implements OnInit {

    isAuthenticated: Boolean = undefined;
    isAdmin: Boolean = undefined;

    environment: string;
    path: string;

    user: User;

    constructor(private principle: PrincipleService,
                private authenticationService: AuthenticationService,
                private location: Location,
                private router: Router) {
        this.environment = window.location.host.split(/[.]/)[0].split(/[-]/)[0];
        this.path = this.location.path();
    }

    ngOnInit() {
        if (this.principle.isIdentityResolved()) {
            this.isAuthenticated = this.principle.isAuthenticated();
            this.isAdmin = this.principle.isInRole('ROLE_ihtsdo-ops-admin') || this.principle.isInRole('ROLE_ims-administrators');
            this.user = this.principle._identity;
        }

        this.principle.loadAccountCompleted.subscribe(() => {
            this.isAuthenticated = this.principle.isAuthenticated();
            this.isAdmin = this.principle.isInRole('ROLE_ihtsdo-ops-admin') || this.principle.isInRole('ROLE_ims-administrators');
        });
    }

    logout() {
        this.authenticationService.logout().then(() => {
            this.router.navigate(['login']);
            window.location.reload();
            this.user = null;
        });
    }
}
