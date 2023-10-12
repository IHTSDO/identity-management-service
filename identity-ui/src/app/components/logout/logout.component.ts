import { Component, OnInit } from '@angular/core';
import { AuthenticationService } from 'src/app/services/authentication/authentication.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
    selector: 'app-logout',
    templateUrl: './logout.component.html',
    styleUrls: ['./logout.component.scss']
})
export class LogoutComponent implements OnInit {

    constructor(private auth: AuthenticationService,
                private router: Router,
                private route: ActivatedRoute) {
    }

    ngOnInit() {
        this.auth.logout().then(() => {
            const returnUrl = this.route.snapshot.queryParamMap.get('serviceReferer');

            if (returnUrl) {
                this.router.navigate(['/login'], {queryParams: {serviceReferer: returnUrl}});
            } else {
                this.router.navigate(['/login']);
            }
        }, (error) => {
            console.error(error);
        });
    }

}
