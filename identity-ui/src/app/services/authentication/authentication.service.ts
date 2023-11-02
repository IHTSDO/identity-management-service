import {Injectable} from '@angular/core';
import {User} from "../../models/user";
import {Observable, Subject, Subscription} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {PrincipleService} from "../principle/principle.service";

@Injectable({
    providedIn: 'root'
})
export class AuthenticationService {

    constructor(private http: HttpClient, private principleService: PrincipleService) {
    }

    login2(credential): Observable<User> {
        return this.http.post<User>('api/authenticate', credential);
    }

    logout2() {
        return this.http.post('api/account/logout', {});
    }

    login(credential) {

        const promise = new Promise<void>((resolve, reject) => {
            this.http.post('api/authenticate', credential).subscribe(() => {
                // login successful
                resolve();
            }, error => {
                console.error('Error while trying to logIn. Error message: ' + error.message);
                reject(error);
            });
        });

        return promise;
    }

    logout() {
        // return this.http.post('api/account/logout', {});

        const promise = new Promise<void>((resolve, reject) => {
            this.http.post('api/account/logout', {}).subscribe(() => {
                this.principleService.authenticate(null);
                resolve();
            }, error => {
                console.error('Error while trying to logout. Error message: ' + error.message);
                reject(error);
            });
        });

        return promise;
    }
}
