import { Injectable, Output, EventEmitter } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { IAccountInfo } from '../models/account-info';

@Injectable({
    providedIn: 'root'
})
export class PrincipleService {

    _identity: IAccountInfo = null;
    _authenticated = false;
    _isIdentityResolved = false;
    @Output() loadAccountCompleted: EventEmitter<boolean> = new EventEmitter();

    constructor(private http: HttpClient) {
    }

    isIdentityResolved() {
        return this._isIdentityResolved;
    }

    isAuthenticated() {
        return this._authenticated;
    }

    isInRole(role: string) {
        if (!this._authenticated || !this._identity || !this._identity.roles) {
            return false;
        }

        return this._identity.roles.indexOf(role) !== -1;
    }

    isInAnyRole(roles: string[]) {
        if (!this._authenticated || !this._identity.roles) {
            return false;
        }

        for (let i = 0; i < roles.length; i++) {
            if (this.isInRole(roles[i])) {
                return true;
            }
        }
        return false;
    }

    authenticate(identity: IAccountInfo) {
        this._identity = identity;
        this._authenticated = identity !== null;
        this.loadAccountCompleted.emit(true);
    }

    identity(force: boolean) {
        const promise = new Promise((resolve, reject) => {
            if (force) {
                this._identity = undefined;
            }

            this.http.get<IAccountInfo>('api/account', {observe: 'response'}).subscribe(response => {
                    this._identity = response.body;
                    this._authenticated = this._identity.login != null;
                    this._isIdentityResolved = true;
                    this.loadAccountCompleted.emit(true);

                    resolve(this._identity);
                },
                msg => {
                    console.error(msg.message);

                    this._identity = null;
                    this._authenticated = false;
                    this._isIdentityResolved = true;
                    this.loadAccountCompleted.emit(true);
                    resolve(this._identity);
                });
        });

        return promise;
    }
}
