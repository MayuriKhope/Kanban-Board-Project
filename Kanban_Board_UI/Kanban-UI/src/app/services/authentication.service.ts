import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  constructor(private http: HttpClient) { }

  saveUser(userDetails: any) {
    return this.http.post(`http://localhost:9000/api/v1/user/save `, userDetails, {
      observe: 'response', withCredentials: true  // withCredentials: true => mean browser can stored data inside the cookies related to thise request
    });
  }

  loginUser(user: any) {
    return this.http.post(`http://localhost:9000/api/v1/user/login`, user, { responseType: 'text', withCredentials: true });
  }
}
