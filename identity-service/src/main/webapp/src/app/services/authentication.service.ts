import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http'
import { PrincipleService } from './principle.service';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {  
  
  constructor(private http : HttpClient, 
              private principle : PrincipleService) { }

  login (credental : object) {
    let promise = new Promise((resolve, reject) => {
      this.http.post('api/authenticate', credental).subscribe((respone) => {        
        // login successful
        resolve()      
      }, error => {
        console.error('Error while trying to logIn. Error message: ' + error.message)
        reject(error)
      });
    });
    
    return promise
  }

  logout () {
    let promise = new Promise((resolve, reject) => {
      this.http.post('api/account/logout', {}).subscribe( () => {
        this.principle.authenticate(null)        
        resolve()   
      }, error => {
        console.error('Error while trying to logout. Error message: ' + error.message)
        reject(error)
      })
    });
    
    return promise
  }

}
